package com.nk.activityreportapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.nk.activityreportapp.presentation.navigation.ActivityReportApp
import com.nk.activityreportapp.ui.theme.ActivityReportAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ActivityReportAppTheme {
                ActivityReportApp()
            }
        }
    }
}