package com.nk.activityreportapp.domain.model

data class AppChange(
    val packageName: String,
    val appName: String,
    val changeType: ChangeType,
    val timestampMs: Long,
)

enum class ChangeType {
    INSTALLED, UNINSTALLED
}
