package com.nk.activityreportapp.domain.usecase

import com.nk.activityreportapp.domain.logic.WeekCalculator
import com.nk.activityreportapp.domain.model.AppDetail
import com.nk.activityreportapp.domain.model.DailyUsage
import com.nk.activityreportapp.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class GetAppDetailUseCase @Inject constructor(
    private val repository: ActivityRepository,
) {
    operator fun invoke(packageName: String, weekStart: LocalDate): Flow<AppDetail> {
        val weekEnd = WeekCalculator.weekEndOf(weekStart)
        return combine(
            repository.observeAppUsage(weekStart, weekEnd),
            repository.observeNotifications(weekStart, weekEnd),
        ) { usage, notifications ->
            val rows = usage.filter { it.packageName == packageName }
            val msByDay = rows.associate { it.date to it.foregroundMs }
            AppDetail(
                packageName = packageName,
                appName = rows.firstOrNull()?.appName ?: packageName,
                totalForegroundMs = rows.sumOf { it.foregroundMs },
                launchCount = rows.sumOf { it.launchCount },
                notificationCount = notifications.filter { it.packageName == packageName }.sumOf { it.count },
                dailyUsage = WeekCalculator.daysOf(weekStart).map { DailyUsage(it, msByDay[it] ?: 0) },
            )
        }
    }
}
