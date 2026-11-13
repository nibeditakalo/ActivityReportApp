package com.nk.activityreportapp.data.repository

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.nk.activityreportapp.data.datasource.UsageStatsDataSource
import com.nk.activityreportapp.domain.model.PermissionState
import com.nk.activityreportapp.domain.repository.PermissionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val usageStats: UsageStatsDataSource,
) : PermissionRepository {

    override fun currentState(): PermissionState = PermissionState(
        usageAccess = usageStats.hasUsageAccess(),
        notificationAccess = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName),
    )
}
