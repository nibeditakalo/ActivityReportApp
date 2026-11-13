package com.nk.activityreportapp.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nk.activityreportapp.R
import com.nk.activityreportapp.domain.model.AppDetail
import com.nk.activityreportapp.presentation.ui.components.AppIcon
import com.nk.activityreportapp.presentation.ui.components.DailyBarChart
import com.nk.activityreportapp.presentation.ui.components.formatDuration
import com.nk.activityreportapp.presentation.viewmodel.AppDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    state: AppDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.appName ?: state.packageName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (detail == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            AppDetailContent(detail, Modifier.padding(padding))
        }
    }
}

@Composable
private fun AppDetailContent(detail: AppDetail, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppIcon(detail.packageName, Modifier.size(56.dp))
            Column {
                Text(text = detail.appName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = detail.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(stringResource(R.string.detail_total), formatDuration(detail.totalForegroundMs))
                HorizontalDivider()
                DetailRow(stringResource(R.string.detail_opens), detail.launchCount.toString())
                HorizontalDivider()
                DetailRow(
                    stringResource(R.string.detail_average_session),
                    formatDuration(detail.averageSessionMs),
                )
                HorizontalDivider()
                DetailRow(stringResource(R.string.detail_notifications), detail.notificationCount.toString())
            }
        }
        Text(text = stringResource(R.string.section_daily), style = MaterialTheme.typography.titleMedium)
        DailyBarChart(days = detail.dailyUsage)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
