package com.nk.activityreportapp.data.datasource

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isVideoApp: Boolean,
)

/**
 * Lists the apps the user can actually open. Android has no "install history" API,
 * so the repository compares this snapshot with the previous one to detect changes.
 */
@Singleton
class InstalledAppsDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun launchableApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { it.activityInfo }
            .distinctBy { it.packageName }
            .map { activity ->
                val app = activity.applicationInfo
                InstalledApp(
                    packageName = activity.packageName,
                    label = app.loadLabel(packageManager).toString(),
                    isVideoApp = app.category == ApplicationInfo.CATEGORY_VIDEO,
                )
            }
    }

    /** Home screens are in the foreground whenever no app is; they are not "usage". */
    fun homePackages(): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.queryIntentActivities(homeIntent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }
}
