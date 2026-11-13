package com.nk.activityreportapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyAppUsageEntity::class,
        DailyUnlocksEntity::class,
        NotificationCountEntity::class,
        KnownAppEntity::class,
        AppChangeEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ActivityDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun notificationDao(): NotificationDao
    abstract fun appDao(): AppDao

    companion object {
        const val NAME = "activity.db"
    }
}
