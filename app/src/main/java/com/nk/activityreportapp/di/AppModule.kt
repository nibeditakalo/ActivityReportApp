package com.nk.activityreportapp.di

import android.content.Context
import androidx.room.Room
import com.nk.activityreportapp.data.local.ActivityDatabase
import com.nk.activityreportapp.data.local.AppDao
import com.nk.activityreportapp.data.local.NotificationDao
import com.nk.activityreportapp.data.local.UsageDao
import com.nk.activityreportapp.data.repository.ActivityRepositoryImpl
import com.nk.activityreportapp.data.repository.PermissionRepositoryImpl
import com.nk.activityreportapp.domain.repository.ActivityRepository
import com.nk.activityreportapp.domain.repository.PermissionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton

/** Dispatchers are injected so tests can replace them with a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(impl: PermissionRepositoryImpl): PermissionRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ActivityDatabase =
        Room.databaseBuilder(context, ActivityDatabase::class.java, ActivityDatabase.NAME).build()

    @Provides
    fun provideUsageDao(database: ActivityDatabase): UsageDao = database.usageDao()

    @Provides
    fun provideNotificationDao(database: ActivityDatabase): NotificationDao = database.notificationDao()

    @Provides
    fun provideAppDao(database: ActivityDatabase): AppDao = database.appDao()

    /** The clock decides "now" and the time zone; tests pass a fixed one. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
