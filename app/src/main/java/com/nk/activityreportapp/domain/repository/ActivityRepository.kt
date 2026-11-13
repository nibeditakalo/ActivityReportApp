package com.nk.activityreportapp.domain.repository

import com.nk.activityreportapp.domain.model.AppChange
import com.nk.activityreportapp.domain.model.AppNotificationCount
import com.nk.activityreportapp.domain.model.DailyAppUsage
import com.nk.activityreportapp.domain.model.DailyCount
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Single source of truth for recorded activity. Reads are cold [Flow]s backed by the
 * local database, so the UI updates by itself whenever a sync writes new rows.
 * All date ranges are inclusive.
 */
interface ActivityRepository {
    fun observeAppUsage(from: LocalDate, to: LocalDate): Flow<List<DailyAppUsage>>
    fun observeUnlocks(from: LocalDate, to: LocalDate): Flow<List<DailyCount>>
    fun observeNotifications(from: LocalDate, to: LocalDate): Flow<List<AppNotificationCount>>
    fun observeAppChanges(from: LocalDate, to: LocalDate): Flow<List<AppChange>>

    /** Copies new system usage events and the installed-app list into the local database. */
    suspend fun sync()

    suspend fun recordNotification(packageName: String, timestampMs: Long)
}
