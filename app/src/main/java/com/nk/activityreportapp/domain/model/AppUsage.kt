package com.nk.activityreportapp.domain.model

import java.time.LocalDate

/** Usage of one app over a whole report period. */
data class AppUsage(
    val packageName: String,
    val appName: String,
    val foregroundMs: Long,
    val launchCount: Int,
)

/** Usage of one app on one calendar day, as stored by the repository. */
data class DailyAppUsage(
    val date: LocalDate,
    val packageName: String,
    val appName: String,
    val foregroundMs: Long,
    val launchCount: Int,
    val isVideoApp: Boolean,
)

/** Total screen time for one calendar day. */
data class DailyUsage(
    val date: LocalDate,
    val foregroundMs: Long,
)

/** A simple per-day counter (for example phone unlocks). */
data class DailyCount(
    val date: LocalDate,
    val count: Int,
)

/** Notifications received from one app on one day. */
data class AppNotificationCount(
    val date: LocalDate,
    val packageName: String,
    val appName: String,
    val count: Int,
)
