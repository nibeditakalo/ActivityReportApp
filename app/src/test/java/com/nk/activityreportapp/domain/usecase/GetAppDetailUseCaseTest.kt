package com.nk.activityreportapp.domain.usecase

import com.nk.activityreportapp.domain.model.AppNotificationCount
import com.nk.activityreportapp.domain.model.DailyAppUsage
import com.nk.activityreportapp.domain.repository.FakeActivityRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GetAppDetailUseCaseTest {
    private val repository = FakeActivityRepository()
    private val useCase = GetAppDetailUseCase(repository)
    private val monday = LocalDate.parse("2023-10-02")
    private val packageName = "com.example.app"

    @Test
    fun `aggregates usage and notifications for a specific package`() = runTest {
        repository.appUsage.value = listOf(
            DailyAppUsage(monday, packageName, "My App", 60_000L, 5, false),
            DailyAppUsage(monday.plusDays(1), packageName, "My App", 30_000L, 2, false),
            DailyAppUsage(monday, "other.pkg", "Other", 100_000L, 1, false)
        )
        repository.notifications.value = listOf(
            AppNotificationCount(monday, packageName, "My App", 10),
            AppNotificationCount(monday, "other.pkg", "Other", 5)
        )

        val detail = useCase(packageName, monday).first()

        assertEquals(packageName, detail.packageName)
        assertEquals("My App", detail.appName)
        assertEquals(90_000L, detail.totalForegroundMs)
        assertEquals(7, detail.launchCount)
        assertEquals(10, detail.notificationCount)
        assertEquals(7, detail.dailyUsage.size)
        assertEquals(60_000L, detail.dailyUsage[0].foregroundMs)
        assertEquals(30_000L, detail.dailyUsage[1].foregroundMs)
    }

    @Test
    fun `returns default app name if no usage data exists`() = runTest {
        val detail = useCase(packageName, monday).first()

        assertEquals(packageName, detail.appName)
        assertEquals(0L, detail.totalForegroundMs)
        assertEquals(0, detail.notificationCount)
    }
}
