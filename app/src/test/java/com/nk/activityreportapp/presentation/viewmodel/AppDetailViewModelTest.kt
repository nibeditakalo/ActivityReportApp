package com.nk.activityreportapp.presentation.viewmodel

import com.nk.activityreportapp.domain.model.DailyAppUsage
import com.nk.activityreportapp.domain.repository.FakeActivityRepository
import com.nk.activityreportapp.domain.usecase.GetAppDetailUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeActivityRepository()
    private val getAppDetail = GetAppDetailUseCase(repository)
    private val packageName = "com.example.app"
    private val monday = LocalDate.parse("2023-10-02")

    private lateinit var viewModel: AppDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AppDetailViewModel(
            packageName = packageName,
            weekStartEpochDay = monday.toEpochDay(),
            getAppDetail = getAppDetail
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertEquals(packageName, state.packageName)
        assertTrue(state.isLoading)
    }

    @Test
    fun `loads app detail when data is available`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        repository.appUsage.value = listOf(
            DailyAppUsage(monday, packageName, "My App", 60_000L, 5, false)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.detail)
        assertEquals("My App", state.detail?.appName)
        assertEquals(60_000L, state.detail?.totalForegroundMs)
    }
}
