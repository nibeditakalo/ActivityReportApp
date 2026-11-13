package com.nk.activityreportapp.domain.logic

import com.nk.activityreportapp.domain.model.DailyUsageTotal
import com.nk.activityreportapp.domain.model.UsageEvent
import com.nk.activityreportapp.domain.model.UsageEventType
import com.nk.activityreportapp.domain.model.UsageSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Rolls sessions and events up into per-day totals in the user's time zone. */
class UsageAggregator @Inject constructor() {

    /**
     * A session that crosses midnight is split so each day gets its own share of the
     * time. The launch is counted once, on the day the session started.
     */
    fun dailyTotals(sessions: List<UsageSession>, zone: ZoneId): List<DailyUsageTotal> {
        val totals = LinkedHashMap<Pair<Long, String>, DailyUsageTotal>()
        for (session in sessions) {
            var cursor = session.startMs
            var isFirstSlice = true
            while (cursor < session.endMs) {
                val date = toLocalDate(cursor, zone)
                val nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val sliceEnd = minOf(session.endMs, nextMidnight)
                val key = date.toEpochDay() to session.packageName
                val previous = totals[key]
                totals[key] = DailyUsageTotal(
                    epochDay = date.toEpochDay(),
                    packageName = session.packageName,
                    foregroundMs = (previous?.foregroundMs ?: 0) + (sliceEnd - cursor),
                    launchCount = (previous?.launchCount ?: 0) + if (isFirstSlice) 1 else 0,
                )
                cursor = sliceEnd
                isFirstSlice = false
            }
        }
        return totals.values.toList()
    }

    /** Number of times the lock screen was dismissed, per day (epoch day to count). */
    fun dailyUnlocks(events: List<UsageEvent>, zone: ZoneId): Map<Long, Int> =
        events.asSequence()
            .filter { it.type == UsageEventType.KEYGUARD_HIDDEN }
            .groupingBy { toLocalDate(it.timestampMs, zone).toEpochDay() }
            .eachCount()

    private fun toLocalDate(epochMs: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
}
