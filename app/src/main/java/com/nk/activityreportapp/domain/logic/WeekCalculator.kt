package com.nk.activityreportapp.domain.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A report week runs Monday to Sunday (ISO-8601), which matches Belgian calendars. */
object WeekCalculator {
    const val DAYS_IN_WEEK = 7

    fun weekStartOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekEndOf(weekStart: LocalDate): LocalDate = weekStart.plusDays(DAYS_IN_WEEK - 1L)

    fun daysOf(weekStart: LocalDate): List<LocalDate> =
        List(DAYS_IN_WEEK) { weekStart.plusDays(it.toLong()) }
}
