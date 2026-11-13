package com.nk.activityreportapp.domain.usecase

import com.nk.activityreportapp.domain.logic.WeeklyReportFactory
import com.nk.activityreportapp.domain.model.DailyAppUsage
import com.nk.activityreportapp.domain.repository.FakeActivityRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GetWeeklyReportUseCaseTest {
    private val repository = FakeActivityRepository()
    private val reportFactory = WeeklyReportFactory()
    private val useCase = GetWeeklyReportUseCase(repository, reportFactory)
    private val monday = LocalDate.parse("2023-10-02")

    @Test
    fun `combines repository data into a report`() = runTest {
        val usage = listOf(
            DailyAppUsage(monday, "pkg1", "App 1", 60_000L, 1, false)
        )
        repository.appUsage.value = usage

        val report = useCase(monday).first()

        assertEquals(monday, report.weekStart)
        assertEquals(60_000L, report.totalForegroundMs)
        assertEquals(1, report.appUsage.size)
        assertEquals("App 1", report.appUsage[0].appName)
    }

    @Test
    fun `includes previous week usage for comparison`() = runTest {
        val lastWeekMonday = monday.minusWeeks(1)
        val usage = listOf(
            DailyAppUsage(lastWeekMonday, "pkg1", "App 1", 50_000L, 1, false),
            DailyAppUsage(monday, "pkg1", "App 1", 60_000L, 1, false)
        )
        repository.appUsage.value = usage

        val report = useCase(monday).first()

        assertEquals(60_000L, report.totalForegroundMs)
        assertEquals(50_000L, report.previousWeekForegroundMs)
    }
}
