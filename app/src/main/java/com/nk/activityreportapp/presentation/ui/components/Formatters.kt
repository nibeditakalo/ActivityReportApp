package com.nk.activityreportapp.presentation.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val MS_PER_MINUTE = 60_000L

/** 7_500_000 ms becomes "2h 5m". Units are kept short so they fit under chart bars. */
fun formatDuration(ms: Long): String {
    val totalMinutes = ms / MS_PER_MINUTE
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        ms > 0 -> "<1m"
        else -> "0m"
    }
}

fun formatDayMonth(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

fun formatWeekdayShort(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

fun formatWeekdayFull(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

fun formatTimestamp(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
