package com.nk.activityreportapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UsageDao {
    @Query(
        """
        SELECT u.epochDay AS epochDay,
               u.packageName AS packageName,
               COALESCE(a.label, u.packageName) AS appName,
               u.foregroundMs AS foregroundMs,
               u.launchCount AS launchCount,
               COALESCE(a.isVideoApp, 0) AS isVideoApp
        FROM daily_app_usage u
        LEFT JOIN known_app a ON a.packageName = u.packageName
        WHERE u.epochDay BETWEEN :fromDay AND :toDay
        """
    )
    abstract fun observeUsage(fromDay: Long, toDay: Long): Flow<List<DailyAppUsageRow>>

    /** The last day that has usage rows; the next sync continues from there. */
    @Query("SELECT MAX(epochDay) FROM daily_app_usage")
    abstract suspend fun latestUsageDay(): Long?

    @Upsert
    abstract suspend fun upsertUsage(rows: List<DailyAppUsageEntity>)

    @Query("SELECT * FROM daily_unlocks WHERE epochDay BETWEEN :fromDay AND :toDay")
    abstract fun observeUnlocks(fromDay: Long, toDay: Long): Flow<List<DailyUnlocksEntity>>

    @Upsert
    abstract suspend fun upsertUnlocks(rows: List<DailyUnlocksEntity>)

    /** Usage and unlocks of one sync are written together or not at all. */
    @Transaction
    open suspend fun saveSync(usage: List<DailyAppUsageEntity>, unlocks: List<DailyUnlocksEntity>) {
        upsertUsage(usage)
        upsertUnlocks(unlocks)
    }
}

@Dao
abstract class NotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfMissing(row: NotificationCountEntity)

    @Query(
        "UPDATE notification_count SET notificationCount = notificationCount + 1 " +
            "WHERE epochDay = :epochDay AND packageName = :packageName"
    )
    abstract suspend fun bump(epochDay: Long, packageName: String)

    @Transaction
    open suspend fun increment(epochDay: Long, packageName: String) {
        insertIfMissing(NotificationCountEntity(epochDay, packageName, 0))
        bump(epochDay, packageName)
    }

    @Query(
        """
        SELECT n.epochDay AS epochDay,
               n.packageName AS packageName,
               COALESCE(a.label, n.packageName) AS appName,
               n.notificationCount AS notificationCount
        FROM notification_count n
        LEFT JOIN known_app a ON a.packageName = n.packageName
        WHERE n.epochDay BETWEEN :fromDay AND :toDay
        """
    )
    abstract fun observeCounts(fromDay: Long, toDay: Long): Flow<List<NotificationCountRow>>
}

@Dao
abstract class AppDao {
    @Query("SELECT * FROM known_app")
    abstract suspend fun getKnownApps(): List<KnownAppEntity>

    @Upsert
    abstract suspend fun upsertApps(apps: List<KnownAppEntity>)

    @Insert
    abstract suspend fun insertChanges(changes: List<AppChangeEntity>)

    @Query(
        "SELECT * FROM app_change WHERE timestampMs >= :fromMs AND timestampMs < :toMs " +
            "ORDER BY timestampMs DESC"
    )
    abstract fun observeChanges(fromMs: Long, toMs: Long): Flow<List<AppChangeEntity>>

    @Transaction
    open suspend fun saveSnapshot(apps: List<KnownAppEntity>, changes: List<AppChangeEntity>) {
        upsertApps(apps)
        if (changes.isNotEmpty()) insertChanges(changes)
    }
}
