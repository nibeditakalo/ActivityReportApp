package com.nk.activityreportapp.domain.repository

import com.nk.activityreportapp.domain.model.AppChange
import com.nk.activityreportapp.domain.model.AppNotificationCount
import com.nk.activityreportapp.domain.model.DailyAppUsage
import com.nk.activityreportapp.domain.model.DailyCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeActivityRepository : ActivityRepository {
    val appUsage = MutableStateFlow<List<DailyAppUsage>>(emptyList())
    val unlocks = MutableStateFlow<List<DailyCount>>(emptyList())
    val notifications = MutableStateFlow<List<AppNotificationCount>>(emptyList())
    val appChanges = MutableStateFlow<List<AppChange>>(emptyList())

    override fun observeAppUsage(from: LocalDate, to: LocalDate): Flow<List<DailyAppUsage>> =
        appUsage.map { list -> list.filter { it.date in from..to } }

    override fun observeUnlocks(from: LocalDate, to: LocalDate): Flow<List<DailyCount>> =
        unlocks.map { list -> list.filter { it.date in from..to } }

    override fun observeNotifications(from: LocalDate, to: LocalDate): Flow<List<AppNotificationCount>> =
        notifications.map { list -> list.filter { it.date in from..to } }

    override fun observeAppChanges(from: LocalDate, to: LocalDate): Flow<List<AppChange>> =
        appChanges.map { list ->
            list.filter {
                val date = java.time.Instant.ofEpochMilli(it.timestampMs)
                    .atZone(java.time.ZoneOffset.UTC)
                    .toLocalDate()
                date in from..to
            }
        }

    var syncCalled = false
    override suspend fun sync() {
        syncCalled = true
    }

    override suspend fun recordNotification(packageName: String, timestampMs: Long) {}
}
