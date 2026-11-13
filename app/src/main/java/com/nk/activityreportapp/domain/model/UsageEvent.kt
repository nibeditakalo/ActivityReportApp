package com.nk.activityreportapp.domain.model

/**
 * Platform-independent copy of the few system usage events the report needs.
 * Keeping it in the domain layer lets the session logic be unit tested on the JVM.
 */
data class UsageEvent(
    val packageName: String,
    val type: UsageEventType,
    val timestampMs: Long,
)

enum class UsageEventType {
    ACTIVITY_RESUMED,
    ACTIVITY_PAUSED,
    SCREEN_OFF,
    KEYGUARD_HIDDEN,
}

/** One continuous stretch of time with [packageName] in the foreground. */
data class UsageSession(
    val packageName: String,
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long get() = endMs - startMs
}

/** Per-day totals produced from sessions, before app names are attached. */
data class DailyUsageTotal(
    val epochDay: Long,
    val packageName: String,
    val foregroundMs: Long,
    val launchCount: Int,
)
