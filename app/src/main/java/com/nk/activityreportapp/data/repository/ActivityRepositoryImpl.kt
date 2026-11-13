package com.nk.activityreportapp.data.repository

import com.nk.activityreportapp.data.datasource.InstalledAppsDataSource
import com.nk.activityreportapp.data.datasource.UsageStatsDataSource
import com.nk.activityreportapp.data.local.AppChangeEntity
import com.nk.activityreportapp.data.local.AppDao
import com.nk.activityreportapp.data.local.DailyAppUsageEntity
import com.nk.activityreportapp.data.local.DailyUnlocksEntity
import com.nk.activityreportapp.data.local.KnownAppEntity
import com.nk.activityreportapp.data.local.NotificationDao
import com.nk.activityreportapp.data.local.UsageDao
import com.nk.activityreportapp.di.IoDispatcher
import com.nk.activityreportapp.domain.logic.SessionBuilder
import com.nk.activityreportapp.domain.logic.UsageAggregator
import com.nk.activityreportapp.domain.model.AppChange
import com.nk.activityreportapp.domain.model.AppNotificationCount
import com.nk.activityreportapp.domain.model.ChangeType
import com.nk.activityreportapp.domain.model.DailyAppUsage
import com.nk.activityreportapp.domain.model.DailyCount
import com.nk.activityreportapp.domain.repository.ActivityRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val usageStats: UsageStatsDataSource,
    private val installedApps: InstalledAppsDataSource,
    private val usageDao: UsageDao,
    private val notificationDao: NotificationDao,
    private val appDao: AppDao,
    private val sessionBuilder: SessionBuilder,
    private val aggregator: UsageAggregator,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ActivityRepository {

    /** The worker and the UI can ask for a sync at the same moment; only one runs at a time. */
    private val syncMutex = Mutex()

    override fun observeAppUsage(from: LocalDate, to: LocalDate): Flow<List<DailyAppUsage>> =
        usageDao.observeUsage(from.toEpochDay(), to.toEpochDay()).map { rows ->
            rows.map {
                DailyAppUsage(
                    date = LocalDate.ofEpochDay(it.epochDay),
                    packageName = it.packageName,
                    appName = it.appName,
                    foregroundMs = it.foregroundMs,
                    launchCount = it.launchCount,
                    isVideoApp = it.isVideoApp,
                )
            }
        }

    override fun observeUnlocks(from: LocalDate, to: LocalDate): Flow<List<DailyCount>> =
        usageDao.observeUnlocks(from.toEpochDay(), to.toEpochDay()).map { rows ->
            rows.map { DailyCount(LocalDate.ofEpochDay(it.epochDay), it.unlockCount) }
        }

    override fun observeNotifications(from: LocalDate, to: LocalDate): Flow<List<AppNotificationCount>> =
        notificationDao.observeCounts(from.toEpochDay(), to.toEpochDay()).map { rows ->
            rows.map {
                AppNotificationCount(
                    date = LocalDate.ofEpochDay(it.epochDay),
                    packageName = it.packageName,
                    appName = it.appName,
                    count = it.notificationCount,
                )
            }
        }

    override fun observeAppChanges(from: LocalDate, to: LocalDate): Flow<List<AppChange>> {
        val zone = clock.zone
        return appDao.observeChanges(
            fromMs = startOfDayMs(from, zone),
            toMs = startOfDayMs(to.plusDays(1), zone),
        ).map { rows ->
            rows.map {
                AppChange(
                    packageName = it.packageName,
                    appName = it.label,
                    changeType = ChangeType.valueOf(it.changeType),
                    timestampMs = it.timestampMs,
                )
            }
        }
    }

    override suspend fun recordNotification(packageName: String, timestampMs: Long) =
        withContext(ioDispatcher) {
            val day = Instant.ofEpochMilli(timestampMs).atZone(clock.zone).toLocalDate()
            notificationDao.increment(day.toEpochDay(), packageName)
        }

    override suspend fun sync() = withContext(ioDispatcher) {
        syncMutex.withLock {
            if (!usageStats.hasUsageAccess()) return@withLock
            val nowMs = clock.millis()
            val zone = clock.zone
            val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
            val earliestDay = today.minusDays(INITIAL_LOOKBACK_DAYS)

            syncInstalledApps(nowMs)

            // Continue from the last day that has data and re-read that whole day: it was
            // only partly recorded last time. The first run asks for two weeks and gets
            // whatever the system still holds.
            val fromDay = usageDao.latestUsageDay()
                ?.let { LocalDate.ofEpochDay(it) }
                ?.coerceIn(earliestDay, today)
                ?: earliestDay
            syncUsage(fromDay, nowMs, zone)
        }
    }

    private suspend fun syncUsage(fromDay: LocalDate, nowMs: Long, zone: ZoneId) {
        val events = usageStats.queryEvents(startOfDayMs(fromDay, zone), nowMs)
        val ignoredPackages = installedApps.homePackages() + SYSTEM_UI_PACKAGES
        val sessions = sessionBuilder.build(events, windowEndMs = nowMs)
            .filterNot { it.packageName in ignoredPackages }

        // Every day in the window is recalculated in full, so its rows replace the stored ones.
        val usage = aggregator.dailyTotals(sessions, zone).map {
            DailyAppUsageEntity(it.epochDay, it.packageName, it.foregroundMs, it.launchCount)
        }
        val unlocks = aggregator.dailyUnlocks(events, zone).map { (epochDay, count) ->
            DailyUnlocksEntity(epochDay, count)
        }
        usageDao.saveSync(usage, unlocks)
    }

    /**
     * Android has no install history, so each sync compares the apps installed now with
     * the list saved by the previous sync. The first run only saves the starting list.
     */
    private suspend fun syncInstalledApps(nowMs: Long) {
        val current = installedApps.launchableApps()
        val currentPackages = current.mapTo(HashSet()) { it.packageName }
        val known = appDao.getKnownApps().associateBy { it.packageName }

        val added = if (known.isEmpty()) emptyList() else current.filter { known[it.packageName]?.installed != true }
        val removed = known.values.filter { it.installed && it.packageName !in currentPackages }

        // The exact moment is unknown: a change is dated at the sync that noticed it.
        val changes = added.map { change(it.packageName, it.label, ChangeType.INSTALLED, nowMs) } +
            removed.map { change(it.packageName, it.label, ChangeType.UNINSTALLED, nowMs) }
        val snapshot = current.map {
            KnownAppEntity(
                it.packageName,
                it.label,
                it.isVideoApp,
                installed = true
            )
        } +
            removed.map { it.copy(installed = false) }
        appDao.saveSnapshot(snapshot, changes)
    }

    private fun change(packageName: String, label: String, type: ChangeType, timestampMs: Long) =
        AppChangeEntity(
            packageName = packageName,
            label = label,
            changeType = type.name,
            timestampMs = timestampMs,
        )

    private fun startOfDayMs(date: LocalDate, zone: ZoneId): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private companion object {
        const val INITIAL_LOOKBACK_DAYS = 14L
        val SYSTEM_UI_PACKAGES = setOf("android", "com.android.systemui")
    }
}
