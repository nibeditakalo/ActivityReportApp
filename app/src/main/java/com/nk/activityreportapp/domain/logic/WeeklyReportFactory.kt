package com.nk.activityreportapp.domain.logic

import com.nk.activityreportapp.domain.model.AppChange
import com.nk.activityreportapp.domain.model.AppCount
import com.nk.activityreportapp.domain.model.AppNotificationCount
import com.nk.activityreportapp.domain.model.AppUsage
import com.nk.activityreportapp.domain.model.DailyAppUsage
import com.nk.activityreportapp.domain.model.DailyCount
import com.nk.activityreportapp.domain.model.DailyUsage
import com.nk.activityreportapp.domain.model.Highlight
import com.nk.activityreportapp.domain.model.WeeklyReport
import java.time.LocalDate
import javax.inject.Inject

/** Pure function from stored rows to the report shown on screen. No Android, no I/O. */
class WeeklyReportFactory @Inject constructor() {

    /**
     * @param usage rows for the report week and, for the comparison, the week before it.
     */
    fun create(
        weekStart: LocalDate,
        usage: List<DailyAppUsage>,
        unlocks: List<DailyCount>,
        notifications: List<AppNotificationCount>,
        appChanges: List<AppChange>,
    ): WeeklyReport {
        val weekEnd = WeekCalculator.weekEndOf(weekStart)
        val thisWeek = usage.filter { it.date in weekStart..weekEnd }
        val previousWeekMs = usage
            .filter { it.date in weekStart.minusWeeks(1)..weekStart.minusDays(1) }
            .sumOf { it.foregroundMs }

        val msByDay = thisWeek.groupBy { it.date }.mapValues { (_, rows) -> rows.sumOf { it.foregroundMs } }
        val dailyUsage = WeekCalculator.daysOf(weekStart).map { DailyUsage(it, msByDay[it] ?: 0) }

        val appUsage = thisWeek.groupBy { it.packageName }
            .map { (packageName, rows) ->
                AppUsage(
                    packageName = packageName,
                    appName = rows.first().appName,
                    foregroundMs = rows.sumOf { it.foregroundMs },
                    launchCount = rows.sumOf { it.launchCount },
                )
            }
            .sortedWith(compareByDescending<AppUsage> { it.foregroundMs }.thenBy { it.appName })

        val totalMs = appUsage.sumOf { it.foregroundMs }
        val weekNotifications = notifications.filter { it.date in weekStart..weekEnd }
        val topNotifier = weekNotifications.groupBy { it.packageName }
            .map { (_, rows) -> AppCount(rows.first().appName, rows.sumOf { it.count }) }
            .maxByOrNull { it.count }

        return WeeklyReport(
            weekStart = weekStart,
            weekEnd = weekEnd,
            totalForegroundMs = totalMs,
            previousWeekForegroundMs = previousWeekMs.takeIf { it > 0 },
            dailyUsage = dailyUsage,
            appUsage = appUsage,
            unlockCount = unlocks.filter { it.date in weekStart..weekEnd }.sumOf { it.count },
            totalNotifications = weekNotifications.sumOf { it.count },
            topNotifier = topNotifier,
            estimatedVideoMs = thisWeek.filter { it.isVideoApp }.sumOf { it.foregroundMs },
            appChanges = appChanges.sortedByDescending { it.timestampMs },
            highlights = highlights(dailyUsage, appUsage, totalMs),
        )
    }

    private fun highlights(
        dailyUsage: List<DailyUsage>,
        appUsage: List<AppUsage>,
        totalMs: Long,
    ): List<Highlight> {
        if (totalMs <= 0) return emptyList()
        val result = ArrayList<Highlight>()
        dailyUsage.maxByOrNull { it.foregroundMs }?.let {
            result += Highlight.BusiestDay(it.date, it.foregroundMs)
        }
        appUsage.firstOrNull()?.let {
            result += Highlight.MostUsedApp(it.appName, (it.foregroundMs * 100 / totalMs).toInt())
        }
        appUsage.maxByOrNull { it.launchCount }?.takeIf { it.launchCount > 0 }?.let {
            result += Highlight.MostOpenedApp(it.appName, it.launchCount)
        }
        return result
    }
}
