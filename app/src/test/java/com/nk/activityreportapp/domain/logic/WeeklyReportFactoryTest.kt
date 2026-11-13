package com.nk.activityreportapp.domain.logic

import com.nk.activityreportapp.domain.model.AppChange
import com.nk.activityreportapp.domain.model.AppCount
import com.nk.activityreportapp.domain.model.AppNotificationCount
import com.nk.activityreportapp.domain.model.ChangeType
import com.nk.activityreportapp.domain.model.DailyAppUsage
import com.nk.activityreportapp.domain.model.DailyCount
import com.nk.activityreportapp.domain.model.Highlight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WeeklyReportFactoryTest {

    private val factory = WeeklyReportFactory()
    private val monday = LocalDate.parse("2026-11-02")

    private fun usage(
        date: LocalDate,
        pkg: String,
        minutes: Long,
        launches: Int = 1,
        video: Boolean = false,
    ) = DailyAppUsage(date, pkg, pkg.substringAfterLast('.'), minutes * MINUTE, launches, video)

    @Test
    fun `week boundaries are Monday to Sunday`() {
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(monday, WeekCalculator.weekStartOf(LocalDate.parse("2026-11-08")))
        assertEquals(monday.plusWeeks(1), WeekCalculator.weekStartOf(LocalDate.parse("2026-11-09")))
        assertEquals(LocalDate.parse("2026-11-08"), WeekCalculator.weekEndOf(monday))
    }

    @Test
    fun `totals, ranking and daily series come from the report week only`() {
        val report = factory.create(
            weekStart = monday,
            usage = listOf(
                usage(monday, MAIL, minutes = 30, launches = 10),
                usage(monday.plusDays(2), MAIL, minutes = 15, launches = 4),
                usage(monday.plusDays(2), VIDEO, minutes = 90, launches = 2, video = true),
                usage(monday.minusDays(1), VIDEO, minutes = 100), // week before
                usage(monday.plusDays(7), VIDEO, minutes = 500), // week after: ignored
            ),
            unlocks = listOf(DailyCount(monday, 40), DailyCount(monday.plusDays(1), 35)),
            notifications = emptyList(),
            appChanges = emptyList(),
        )

        assertEquals(135 * MINUTE, report.totalForegroundMs)
        assertEquals(100 * MINUTE, report.previousWeekForegroundMs)
        assertEquals(listOf("video", "mail"), report.appUsage.map { it.appName })
        assertEquals(14, report.appUsage.last().launchCount)
        assertEquals(7, report.dailyUsage.size)
        assertEquals(30 * MINUTE, report.dailyUsage[0].foregroundMs)
        assertEquals(0L, report.dailyUsage[1].foregroundMs)
        assertEquals(105 * MINUTE, report.dailyUsage[2].foregroundMs)
        assertEquals(90 * MINUTE, report.estimatedVideoMs)
        assertEquals(75, report.unlockCount)
        assertTrue(report.hasData)
    }

    @Test
    fun `highlights name the busiest day, most used and most opened app`() {
        val report = factory.create(
            weekStart = monday,
            usage = listOf(
                usage(monday, MAIL, minutes = 25, launches = 30),
                usage(monday.plusDays(4), VIDEO, minutes = 75, launches = 3),
            ),
            unlocks = emptyList(),
            notifications = emptyList(),
            appChanges = emptyList(),
        )

        assertEquals(
            listOf(
                Highlight.BusiestDay(monday.plusDays(4), 75 * MINUTE),
                Highlight.MostUsedApp("video", 75),
                Highlight.MostOpenedApp("mail", 30),
            ),
            report.highlights,
        )
    }

    @Test
    fun `notifications are summed and the noisiest app is identified`() {
        val report = factory.create(
            weekStart = monday,
            usage = emptyList(),
            unlocks = emptyList(),
            notifications = listOf(
                AppNotificationCount(monday, MAIL, "Mail", 12),
                AppNotificationCount(monday.plusDays(1), MAIL, "Mail", 8),
                AppNotificationCount(monday, VIDEO, "Video", 15),
            ),
            appChanges = listOf(
                AppChange(VIDEO, "Video", ChangeType.INSTALLED, 1_000),
                AppChange(MAIL, "Mail", ChangeType.UNINSTALLED, 2_000),
            ),
        )

        assertEquals(35, report.totalNotifications)
        assertEquals(AppCount("Mail", 20), report.topNotifier)
        assertEquals(listOf(ChangeType.UNINSTALLED, ChangeType.INSTALLED), report.appChanges.map { it.changeType })
    }

    @Test
    fun `an empty week has no data, no comparison and no highlights`() {
        val report = factory.create(monday, emptyList(), emptyList(), emptyList(), emptyList())

        assertFalse(report.hasData)
        assertNull(report.previousWeekForegroundMs)
        assertNull(report.topNotifier)
        assertTrue(report.highlights.isEmpty())
        assertEquals(7, report.dailyUsage.size)
    }

    private companion object {
        const val MAIL = "com.example.mail"
        const val VIDEO = "com.example.video"
        const val MINUTE = 60_000L
    }
}
