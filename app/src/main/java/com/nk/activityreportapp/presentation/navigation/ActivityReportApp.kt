package com.nk.activityreportapp.presentation.navigation

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nk.activityreportapp.presentation.ui.ActivityReportScreen
import com.nk.activityreportapp.presentation.ui.AppDetailScreen
import com.nk.activityreportapp.presentation.ui.OnboardingScreen
import com.nk.activityreportapp.presentation.viewmodel.ActivityViewModel
import com.nk.activityreportapp.presentation.viewmodel.AppDetailViewModel
import com.nk.activityreportapp.presentation.viewmodel.MainViewModel

/**
 * Root of the UI. Without usage access there is nothing to show, so the app gates on it:
 * onboarding first, the report once access is granted.
 */
@Composable
fun ActivityReportApp(mainViewModel: MainViewModel = hiltViewModel()) {
    val permissions by mainViewModel.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Special access is granted in system Settings, so re-check every time the app returns.
    LifecycleResumeEffect(Unit) {
        mainViewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    if (!permissions.usageAccess) {
        OnboardingScreen(
            onGrantAccess = { context.openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS) },
        )
        return
    }

    val backStack = rememberSaveable(saver = BackStackSaver) { mutableStateListOf<Any>(Route.Report) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        // The second decorator gives every back stack entry its own ViewModelStore,
        // so a ViewModel lives exactly as long as its screen is on the stack.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Route.Report> {
                val viewModel = hiltViewModel<ActivityViewModel>()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                ActivityReportScreen(
                    state = state,
                    notificationAccess = permissions.notificationAccess,
                    onPreviousWeek = viewModel::onPreviousWeek,
                    onNextWeek = viewModel::onNextWeek,
                    onRefresh = viewModel::onRefresh,
                    onAppClick = { packageName ->
                        backStack.add(Route.AppDetail(packageName, state.weekStart.toEpochDay()))
                    },
                    onEnableNotifications = {
                        context.openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    },
                )
            }
            entry<Route.AppDetail> { key ->
                val viewModel = hiltViewModel<AppDetailViewModel, AppDetailViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key.packageName, key.weekStartEpochDay) },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                AppDetailScreen(
                    state = state,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}

private val BackStackSaver = listSaver<SnapshotStateList<Any>, String>(
    save = { stack -> stack.filterIsInstance<Route>().map { it.encode() } },
    restore = { saved ->
        mutableStateListOf<Any>().apply { addAll(saved.map { Route.Companion.decode(it) }) }
    },
)

private fun Context.openSettings(action: String) {
    runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
