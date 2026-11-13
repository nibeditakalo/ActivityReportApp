package com.nk.activityreportapp.presentation.viewmodel

import com.nk.activityreportapp.domain.logic.WeeklyReportFactory
import com.nk.activityreportapp.domain.model.DailyAppUsage
import com.nk.activityreportapp.domain.repository.FakeActivityRepository
import com.nk.activityreportapp.domain.usecase.GetWeeklyReportUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeActivityRepository()
    private val reportFactory = WeeklyReportFactory()
    private val getWeeklyReport = GetWeeklyReportUseCase(repository, reportFactory)
    private val fixedClock = Clock.fixed(Instant.parse("2023-10-05T12:00:00Z"), ZoneId.of("UTC"))
    private val today = LocalDate.now(fixedClock)
    private val monday = LocalDate.parse("2023-10-02") // Monday of that week

    private lateinit var viewModel: ActivityViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ActivityViewModel(getWeeklyReport, repository, fixedClock)

    @Test
    fun `initial state shows current week and starts loading`() = runTest {
        viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        
        val state = viewModel.uiState.value
        assertEquals(monday, state.weekStart)
        assertTrue(state.isCurrentWeek)
        assertTrue(state.isLoading)
    }

    @Test
    fun `navigation changes selected week`() = runTest {
        viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onPreviousWeek()
        runCurrent() // Trigger flatMapLatest
        
        assertEquals(monday.minusWeeks(1), viewModel.uiState.value.weekStart)
        assertFalse(viewModel.uiState.value.isCurrentWeek)

        viewModel.onNextWeek()
        runCurrent()
        
        assertEquals(monday, viewModel.uiState.value.weekStart)
        assertTrue(viewModel.uiState.value.isCurrentWeek)
    }

    @Test
    fun `successful sync updates state`() = runTest {
        viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        // ViewModel calls onRefresh in init.
        // We use runCurrent() to allow the launch in onRefresh to start.
        runCurrent()
        
        // If the sync() in FakeActivityRepository is very fast (suspend fun sync() {}), 
        // it might finish immediately.
        // Let's check if sync was at least called.
        assertTrue(repository.syncCalled)
        
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isSyncing)
        assertFalse(viewModel.uiState.value.syncFailed)
    }

    @Test
    fun `loads report data from repository`() = runTest {
        viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        repository.appUsage.value = listOf(
            DailyAppUsage(monday, "pkg", "App", 1000L, 1, false)
        )
        
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.report)
        assertEquals(1000L, state.report?.totalForegroundMs)
    }
}
