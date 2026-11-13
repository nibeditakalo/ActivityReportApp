package com.nk.activityreportapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nk.activityreportapp.domain.repository.ActivityRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies usage events into the local database in the background.
 *
 * The system keeps detailed events for a limited time only, so a weekly report is
 * possible only if they are collected regularly. A periodic WorkManager job does this
 * without a foreground service or a permanent notification, and Android is free to
 * batch it with other work to save battery.
 *
 * Note: this job matters only when the app has not been opened for a while. The app also
 * syncs every time it is opened, so the periodic job just fills the gaps between visits.
 */
@HiltWorker
class UsageSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: ActivityRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        repository.sync()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}

@Singleton
class UsageSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun schedule() {
        val request =
            PeriodicWorkRequestBuilder<UsageSyncWorker>(SYNC_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val WORK_NAME = "usage-sync"
        const val SYNC_INTERVAL_HOURS = 12L
    }
}
