package com.nk.activityreportapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.activityreportapp.domain.logic.WeekCalculator
import com.nk.activityreportapp.domain.model.WeeklyReport
import com.nk.activityreportapp.domain.repository.ActivityRepository
import com.nk.activityreportapp.domain.usecase.GetWeeklyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Everything the report screen needs to draw itself, as one immutable value. */
data class ActivityUiState(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val isCurrentWeek: Boolean,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val syncFailed: Boolean = false,
    val report: WeeklyReport? = null,
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val getWeeklyReport: GetWeeklyReportUseCase,
    private val repository: ActivityRepository,
    clock: Clock,
) : ViewModel() {

    private val currentWeekStart = WeekCalculator.weekStartOf(LocalDate.now(clock))
    private val selectedWeek = MutableStateFlow(currentWeekStart)
    private val isSyncing = MutableStateFlow(false)
    private val syncFailed = MutableStateFlow(false)

    /** Switching week cancels the previous database query and starts the new one. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val weekReport = selectedWeek.flatMapLatest { week ->
        getWeeklyReport(week)
            .map { report -> week to report as WeeklyReport? }
            .onStart { emit(week to null) }
    }

    val uiState: StateFlow<ActivityUiState> =
        combine(weekReport, isSyncing, syncFailed) { (week, report), syncing, failed ->
            ActivityUiState(
                weekStart = week,
                weekEnd = WeekCalculator.weekEndOf(week),
                isCurrentWeek = week == currentWeekStart,
                isLoading = report == null,
                isSyncing = syncing,
                syncFailed = failed,
                report = report,
            )
        }.stateIn(
            scope = viewModelScope,
            // Keeps the query alive across a rotation, stops it 5 s after the UI goes away.
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ActivityUiState(
                weekStart = currentWeekStart,
                weekEnd = WeekCalculator.weekEndOf(currentWeekStart),
                isCurrentWeek = true,
            ),
        )

    init {
        onRefresh()
    }

    fun onPreviousWeek() = selectedWeek.update { it.minusWeeks(1) }

    fun onNextWeek() = selectedWeek.update { if (it < currentWeekStart) it.plusWeeks(1) else it }

    fun onRefresh() {
        if (isSyncing.value) return
        isSyncing.value = true
        viewModelScope.launch {
            try {
                repository.sync()
                syncFailed.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                syncFailed.value = true
            } finally {
                isSyncing.value = false
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
