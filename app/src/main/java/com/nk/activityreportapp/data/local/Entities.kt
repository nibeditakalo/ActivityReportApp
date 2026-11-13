package com.nk.activityreportapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Days are stored as epoch days (LocalDate.toEpochDay) so range queries are plain integer comparisons. */
@Entity(tableName = "daily_app_usage", primaryKeys = ["epochDay", "packageName"])
data class DailyAppUsageEntity(
    val epochDay: Long,
    val packageName: String,
    val foregroundMs: Long,
    val launchCount: Int,
)

@Entity(tableName = "daily_unlocks")
data class DailyUnlocksEntity(
    @PrimaryKey val epochDay: Long,
    val unlockCount: Int,
)

@Entity(tableName = "notification_count", primaryKeys = ["epochDay", "packageName"])
data class NotificationCountEntity(
    val epochDay: Long,
    val packageName: String,
    val notificationCount: Int,
)

/**
 * Every launchable app seen so far. Rows are kept after an uninstallation (installed = false)
 * so older reports can still show the app's name.
 */
@Entity(tableName = "known_app")
data class KnownAppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val isVideoApp: Boolean,
    val installed: Boolean,
)

@Entity(tableName = "app_change")
data class AppChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val label: String,
    val changeType: String,
    val timestampMs: Long,
)

/** Query results that join usage or notification rows with the app label. */
data class DailyAppUsageRow(
    val epochDay: Long,
    val packageName: String,
    val appName: String,
    val foregroundMs: Long,
    val launchCount: Int,
    val isVideoApp: Boolean,
)

data class NotificationCountRow(
    val epochDay: Long,
    val packageName: String,
    val appName: String,
    val notificationCount: Int,
)
