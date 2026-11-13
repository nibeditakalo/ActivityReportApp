package com.nk.activityreportapp.data.datasource

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.nk.activityreportapp.domain.model.UsageEvent
import com.nk.activityreportapp.domain.model.UsageEventType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Thin wrapper around [UsageStatsManager]; the only class that knows the system event codes. */
@Singleton
class UsageStatsDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** "Usage access" is an app-op the user toggles in Settings, not a runtime permission. */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Returns the events the report needs, oldest first. Empty when access is missing. */
    fun queryEvents(fromMs: Long, toMs: Long): List<UsageEvent> {
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val systemEvents = manager.queryEvents(fromMs, toMs) ?: return emptyList()
        val result = ArrayList<UsageEvent>()
        val event = UsageEvents.Event()
        while (systemEvents.hasNextEvent()) {
            systemEvents.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.ACTIVITY_RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventType.ACTIVITY_PAUSED
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.SCREEN_OFF
                UsageEvents.Event.KEYGUARD_HIDDEN -> UsageEventType.KEYGUARD_HIDDEN
                else -> continue
            }
            result += UsageEvent(event.packageName.orEmpty(), type, event.timeStamp)
        }
        return result
    }
}
