package com.nk.activityreportapp.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nk.activityreportapp.domain.model.DailyUsage
import com.nk.activityreportapp.presentation.ui.components.formatDuration
import com.nk.activityreportapp.presentation.ui.components.formatWeekdayFull
import com.nk.activityreportapp.presentation.ui.components.formatWeekdayShort

/**
 * Seven bars, one per day. Built from plain layout composables instead of a chart
 * library: the app needs exactly one chart type, and this keeps it themed and
 * readable by TalkBack for free.
 */
@Composable
fun DailyBarChart(days: List<DailyUsage>, modifier: Modifier = Modifier) {
    val maxMs = days.maxOfOrNull { it.foregroundMs }?.coerceAtLeast(1L) ?: 1L
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEach { day ->
            val fraction = (day.foregroundMs.toFloat() / maxMs).coerceIn(0.02f, 1f)
            val duration = formatDuration(day.foregroundMs)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${formatWeekdayFull(day.date)}: $duration"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (day.foregroundMs > 0) duration else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (day.foregroundMs > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                    )
                }
                Text(
                    text = formatWeekdayShort(day.date),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
}
