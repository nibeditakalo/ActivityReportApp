package com.nk.activityreportapp.domain.logic

import com.nk.activityreportapp.domain.model.UsageEvent
import com.nk.activityreportapp.domain.model.UsageEventType
import com.nk.activityreportapp.domain.model.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class UsageAggregatorTest {
    private val aggregator = UsageAggregator()
    private val zone = ZoneId.of("UTC")

    @Test
    fun `dailyTotals splits session at midnight`() {
        val startOfToday = LocalDate.of(2023, 10, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val session = UsageSession(
            packageName = "pkg1",
            startMs = startOfToday - 1000L, // 1 second before midnight
            endMs = startOfToday + 1000L    // 1 second after midnight
        )

        val totals = aggregator.dailyTotals(listOf(session), zone)

        assertEquals(2, totals.size)
        val day1 = totals.find { it.epochDay == LocalDate.of(2023, 9, 30).toEpochDay() }!!
        val day2 = totals.find { it.epochDay == LocalDate.of(2023, 10, 1).toEpochDay() }!!

        assertEquals(1000L, day1.foregroundMs)
        assertEquals(1, day1.launchCount)

        assertEquals(1000L, day2.foregroundMs)
        assertEquals(0, day2.launchCount) // Launch only counted on the first day
    }

    @Test
    fun `dailyTotals aggregates multiple sessions for same package`() {
        val today = LocalDate.of(2023, 10, 1)
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val sessions = listOf(
            UsageSession("pkg1", startOfToday + 1000L, startOfToday + 2000L),
            UsageSession("pkg1", startOfToday + 3000L, startOfToday + 5000L)
        )

        val totals = aggregator.dailyTotals(sessions, zone)

        assertEquals(1, totals.size)
        assertEquals(3000L, totals[0].foregroundMs)
        assertEquals(2, totals[0].launchCount)
    }

    @Test
    fun `dailyUnlocks counts KEYGUARD_HIDDEN events per day`() {
        val day1 = LocalDate.of(2023, 10, 1)
        val day2 = LocalDate.of(2023, 10, 2)
        val t1 = day1.atStartOfDay(zone).toInstant().toEpochMilli()
        val t2 = day2.atStartOfDay(zone).toInstant().toEpochMilli()

        val events = listOf(
            UsageEvent("any", UsageEventType.KEYGUARD_HIDDEN, t1 + 100),
            UsageEvent("any", UsageEventType.KEYGUARD_HIDDEN, t1 + 200),
            UsageEvent("any", UsageEventType.ACTIVITY_RESUMED, t1 + 300),
            UsageEvent("any", UsageEventType.KEYGUARD_HIDDEN, t2 + 100)
        )

        val unlocks = aggregator.dailyUnlocks(events, zone)

        assertEquals(2, unlocks.size)
        assertEquals(2, unlocks[day1.toEpochDay()])
        assertEquals(1, unlocks[day2.toEpochDay()])
    }
}
