package com.nk.activityreportapp.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nk.activityreportapp.R
import com.nk.activityreportapp.ui.theme.ActivityReportAppTheme

@Composable
fun OnboardingScreen(onGrantAccess: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            InfoCard(
                title = stringResource(R.string.onboarding_permission_title),
                body = stringResource(R.string.onboarding_permission_body),
            )
            InfoCard(
                title = stringResource(R.string.onboarding_privacy_title),
                body = stringResource(R.string.onboarding_privacy_body),
            )
            Button(onClick = onGrantAccess, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_grant))
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true, name = "Onboarding Light Mode")
@Composable
fun OnboardingScreenPreview() {
    ActivityReportAppTheme {
        OnboardingScreen(
            onGrantAccess = {}
        )
    }
}
