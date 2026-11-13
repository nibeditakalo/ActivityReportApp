package com.nk.activityreportapp.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nk.activityreportapp.R
import com.nk.activityreportapp.domain.model.AppChange
import com.nk.activityreportapp.domain.model.AppUsage
import com.nk.activityreportapp.domain.model.ChangeType
import com.nk.activityreportapp.domain.model.Highlight
import com.nk.activityreportapp.domain.model.WeeklyReport
import com.nk.activityreportapp.presentation.ui.components.AppIcon
import com.nk.activityreportapp.presentation.ui.components.DailyBarChart
import com.nk.activityreportapp.presentation.ui.components.formatDayMonth
import com.nk.activityreportapp.presentation.ui.components.formatDuration
import com.nk.activityreportapp.presentation.ui.components.formatTimestamp
import com.nk.activityreportapp.presentation.ui.components.formatWeekdayFull
import com.nk.activityreportapp.presentation.viewmodel.ActivityUiState
import kotlin.math.abs

/**
 * Stateless: it receives the state and reports user actions upwards.
 * That makes it easy to preview and to test without a ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityReportScreen(
    state: ActivityUiState,
    notificationAccess: Boolean,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onRefresh: () -> Unit,
    onAppClick: (String) -> Unit,
    onEnableNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_title)) },
                actions = {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Filled.Refresh, stringResource(R.string.action_refresh))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val report = state.report
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "week") {
                WeekSelector(state, onPreviousWeek, onNextWeek)
            }
            if (state.syncFailed) {
                item(key = "sync_failed") {
                    Text(
                        text = stringResource(R.string.sync_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            when {
                report == null -> item(key = "loading") {
                    Box(Modifier
                        .fillMaxWidth()
                        .padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                !report.hasData -> item(key = "empty") { EmptyWeek() }

                else -> reportItems(report, notificationAccess, onAppClick, onEnableNotifications)
            }
        }
    }
}

private fun LazyListScope.reportItems(
    report: WeeklyReport,
    notificationAccess: Boolean,
    onAppClick: (String) -> Unit,
    onEnableNotifications: () -> Unit,
) {
    item(key = "summary") { SummaryCard(report) }
    item(key = "stats") { StatRow(report, notificationAccess) }
    if (!notificationAccess) {
        item(key = "notifications_cta") { NotificationsCta(onEnableNotifications) }
    }
    if (report.highlights.isNotEmpty()) {
        item(key = "highlights") { HighlightsCard(report) }
    }
    if (report.appUsage.isNotEmpty()) {
        item(key = "apps_header") { SectionTitle(stringResource(R.string.section_apps)) }
        val longestMs = report.appUsage.first().foregroundMs.coerceAtLeast(1L)
        items(report.appUsage, key = { it.packageName }) { usage ->
            AppUsageItem(usage, longestMs, onClick = { onAppClick(usage.packageName) })
        }
    }
    item(key = "changes_header") { SectionTitle(stringResource(R.string.section_changes)) }
    if (report.appChanges.isEmpty()) {
        item(key = "changes_none") {
            Text(
                text = stringResource(R.string.changes_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        items(report.appChanges, key = { "${it.packageName}-${it.changeType}-${it.timestampMs}" }) {
            AppChangeItem(it)
        }
    }
    item(key = "footer") {
        Text(
            text = stringResource(R.string.footer_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun WeekSelector(state: ActivityUiState, onPreviousWeek: () -> Unit, onNextWeek: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                stringResource(R.string.action_previous_week),
            )
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(
                    R.string.week_range,
                    formatDayMonth(state.weekStart),
                    formatDayMonth(state.weekEnd),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.isCurrentWeek) {
                Text(
                    text = stringResource(R.string.this_week),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        IconButton(onClick = onNextWeek, enabled = !state.isCurrentWeek) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                stringResource(R.string.action_next_week),
            )
        }
    }
}

@Composable
private fun SummaryCard(report: WeeklyReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.summary_screen_time),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(report.totalForegroundMs),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = comparisonText(report), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.summary_daily_average, formatDuration(report.dailyAverageMs)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DailyBarChart(days = report.dailyUsage, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun comparisonText(report: WeeklyReport): String {
    val previous = report.previousWeekForegroundMs
        ?: return stringResource(R.string.summary_no_previous_week)
    val percent = ((report.totalForegroundMs - previous) * 100 / previous).toInt()
    return when {
        percent > 0 -> stringResource(R.string.summary_vs_last_week_up, percent)
        percent < 0 -> stringResource(R.string.summary_vs_last_week_down, abs(percent))
        else -> stringResource(R.string.summary_vs_last_week_same)
    }
}

@Composable
private fun StatRow(report: WeeklyReport, notificationAccess: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            label = stringResource(R.string.stat_unlocks),
            value = report.unlockCount.toString(),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.stat_notifications),
            // Counts recorded while access was on are still shown after it is turned off.
            value = if (notificationAccess || report.totalNotifications > 0) {
                report.totalNotifications.toString()
            } else {
                stringResource(R.string.stat_notifications_off)
            },
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.stat_video),
            value = formatDuration(report.estimatedVideoMs),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotificationsCta(onEnableNotifications: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notifications_cta_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.notifications_cta_body),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onEnableNotifications) {
                Text(stringResource(R.string.notifications_cta_action))
            }
        }
    }
}

@Composable
private fun HighlightsCard(report: WeeklyReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.section_highlights),
                style = MaterialTheme.typography.titleMedium,
            )
            report.highlights.forEach { highlight ->
                Text(text = highlightText(highlight), style = MaterialTheme.typography.bodyMedium)
            }
            report.topNotifier?.let {
                Text(
                    text = stringResource(R.string.top_notifier, it.appName, it.count),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun highlightText(highlight: Highlight): String = when (highlight) {
    is Highlight.BusiestDay -> stringResource(
        R.string.highlight_busiest_day,
        formatWeekdayFull(highlight.date),
        formatDuration(highlight.foregroundMs),
    )

    is Highlight.MostUsedApp -> stringResource(
        R.string.highlight_most_used,
        highlight.appName,
        highlight.sharePercent,
    )

    is Highlight.MostOpenedApp -> stringResource(
        R.string.highlight_most_opened,
        highlight.appName,
        highlight.launchCount,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun AppUsageItem(usage: AppUsage, longestMs: Long, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(usage.packageName, Modifier.size(40.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = usage.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatDuration(usage.foregroundMs),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            LinearProgressIndicator(
                progress = { usage.foregroundMs.toFloat() / longestMs },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.app_opens, usage.launchCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppChangeItem(change: AppChange) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(change.packageName, Modifier.size(32.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = change.appName, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                text = formatTimestamp(change.timestampMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(
                if (change.changeType == ChangeType.INSTALLED) R.string.change_installed
                else R.string.change_uninstalled
            ),
            style = MaterialTheme.typography.labelLarge,
            color = if (change.changeType == ChangeType.INSTALLED) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun EmptyWeek() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}