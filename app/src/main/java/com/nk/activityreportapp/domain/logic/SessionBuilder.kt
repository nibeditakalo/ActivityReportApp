package com.nk.activityreportapp.domain.logic

import com.nk.activityreportapp.domain.model.UsageEvent
import com.nk.activityreportapp.domain.model.UsageEventType
import com.nk.activityreportapp.domain.model.UsageSession
import javax.inject.Inject

/**
 * Turns the raw resume/pause event stream into foreground sessions.
 *
 * Why not use the system's aggregated usage buckets? They are cut on the system's own
 * boundaries (not the user's local midnight or week) and carry no launch count.
 * Pairing the events ourselves gives exact per-day numbers.
 *
 * Rules:
 *  - A session opens when an activity of a package is resumed.
 *  - It closes when that package is paused, another package is resumed,
 *    or the screen turns off.
 *  - Moving between two screens of the same app produces pause + resume a few
 *    milliseconds apart; sessions of the same package closer than [mergeGapMs] are
 *    merged so this counts as one visit, not two launches.
 *  - A session still open at the end of the window is the app in use right now and
 *    is closed at [windowEndMs].
 */
class SessionBuilder @Inject constructor() {

    fun build(
        events: List<UsageEvent>, // oldest first, as the system returns them
        windowEndMs: Long,
        mergeGapMs: Long = DEFAULT_MERGE_GAP_MS,
    ): List<UsageSession> {
        val sessions = ArrayList<UsageSession>()
        var currentPackage: String? = null
        var currentStart = 0L

        fun close(endMs: Long) {
            val pkg = currentPackage ?: return
            currentPackage = null
            if (endMs <= currentStart) return
            val last = sessions.lastOrNull()
            if (last != null && last.packageName == pkg && currentStart - last.endMs <= mergeGapMs) {
                sessions[sessions.lastIndex] = last.copy(endMs = endMs)
            } else {
                sessions += UsageSession(pkg, currentStart, endMs)
            }
        }

        for (event in events) {
            when (event.type) {
                UsageEventType.ACTIVITY_RESUMED -> if (currentPackage != event.packageName) {
                    close(event.timestampMs)
                    currentPackage = event.packageName
                    currentStart = event.timestampMs
                }

                UsageEventType.ACTIVITY_PAUSED -> if (currentPackage == event.packageName) {
                    close(event.timestampMs)
                }

                UsageEventType.SCREEN_OFF -> close(event.timestampMs)

                // Unlocks are counted separately in UsageAggregator.
                UsageEventType.KEYGUARD_HIDDEN -> Unit
            }
        }
        close(windowEndMs)
        return sessions
    }

    companion object {
        const val DEFAULT_MERGE_GAP_MS = 1_500L
    }
}
