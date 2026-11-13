package com.nk.activityreportapp.domain.model

import java.time.LocalDate

data class WeeklyReport(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val totalForegroundMs: Long,
    /** Null when nothing was recorded for the week before, so no comparison is shown. */
    val previousWeekForegroundMs: Long?,
    /** Always seven entries, Monday to Sunday, zero-filled. */
    val dailyUsage: List<DailyUsage>,
    /** Sorted by time spent, longest first. */
    val appUsage: List<AppUsage>,
    val unlockCount: Int,
    val totalNotifications: Int,
    val topNotifier: AppCount?,
    /** Estimate: time spent in apps that declare the "video" category. */
    val estimatedVideoMs: Long,
    val appChanges: List<AppChange>,
    val highlights: List<Highlight>,
) {
    val hasData: Boolean
        get() = totalForegroundMs > 0 || unlockCount > 0 || totalNotifications > 0 || appChanges.isNotEmpty()

    val dailyAverageMs: Long
        get() = if (dailyUsage.isEmpty()) 0 else totalForegroundMs / dailyUsage.size
}

data class AppCount(
    val appName: String,
    val count: Int,
)

/**
 * Facts worth calling out. They carry data only; the UI turns them into
 * localised sentences, so the domain layer holds no display strings.
 */
sealed interface Highlight {
    data class BusiestDay(val date: LocalDate, val foregroundMs: Long) : Highlight
    data class MostUsedApp(val appName: String, val sharePercent: Int) : Highlight
    data class MostOpenedApp(val appName: String, val launchCount: Int) : Highlight
}

/** Everything the detail screen shows for one app in one week. */
data class AppDetail(
    val packageName: String,
    val appName: String,
    val totalForegroundMs: Long,
    val launchCount: Int,
    val notificationCount: Int,
    val dailyUsage: List<DailyUsage>,
) {
    val averageSessionMs: Long
        get() = if (launchCount == 0) 0 else totalForegroundMs / launchCount
}

data class PermissionState(
    val usageAccess: Boolean,
    val notificationAccess: Boolean,
)
