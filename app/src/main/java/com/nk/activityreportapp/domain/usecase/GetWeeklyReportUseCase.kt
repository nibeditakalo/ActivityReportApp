package com.nk.activityreportapp.domain.usecase

import com.nk.activityreportapp.domain.logic.WeekCalculator
import com.nk.activityreportapp.domain.logic.WeeklyReportFactory
import com.nk.activityreportapp.domain.model.WeeklyReport
import com.nk.activityreportapp.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Streams the report for the week starting on [invoke]'s `weekStart` (a Monday).
 * It re-emits whenever a background sync or the notification counter writes new data.
 */
class GetWeeklyReportUseCase @Inject constructor(
    private val repository: ActivityRepository,
    private val reportFactory: WeeklyReportFactory,
) {
    operator fun invoke(weekStart: LocalDate): Flow<WeeklyReport> {
        val weekEnd = WeekCalculator.weekEndOf(weekStart)
        return combine(
            // One extra week of usage feeds the "compared with the week before" figure.
            repository.observeAppUsage(weekStart.minusWeeks(1), weekEnd),
            repository.observeUnlocks(weekStart, weekEnd),
            repository.observeNotifications(weekStart, weekEnd),
            repository.observeAppChanges(weekStart, weekEnd),
        ) { usage, unlocks, notifications, changes ->
            reportFactory.create(weekStart, usage, unlocks, notifications, changes)
        }
    }
}
