package com.nk.activityreportapp.data.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nk.activityreportapp.domain.repository.ActivityRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Counts notifications per app. It runs only if the user grants notification access.
 *
 * Privacy: the title and text of a notification are never read. The only things
 * stored are the posting app's package name and the day.
 */
@AndroidEntryPoint
class NotificationCounterService : NotificationListenerService() {

    @Inject lateinit var repository: ActivityRepository

    private lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val postedBy = sbn.packageName
        val postedAt = sbn.postTime
        scope.launch { repository.recordNotification(postedBy, postedAt) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
