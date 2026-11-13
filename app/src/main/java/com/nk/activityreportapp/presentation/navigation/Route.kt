package com.nk.activityreportapp.presentation.navigation

/** Navigation 3 keys. Each key is plain data describing one screen on the back stack. */
sealed interface Route {
    data object Report : Route

    data class AppDetail(val packageName: String, val weekStartEpochDay: Long) : Route

    /** Compact text form so the back stack survives rotation and process death. */
    fun encode(): String = when (this) {
        Report -> REPORT
        is AppDetail -> "$DETAIL$SEPARATOR$weekStartEpochDay$SEPARATOR$packageName"
    }

    companion object {
        private const val REPORT = "report"
        private const val DETAIL = "detail"
        private const val SEPARATOR = "|"

        fun decode(value: String): Route {
            val parts = value.split(SEPARATOR, limit = 3)
            val epochDay = parts.getOrNull(1)?.toLongOrNull()
            val packageName = parts.getOrNull(2)
            return if (parts[0] == DETAIL && epochDay != null && packageName != null) {
                AppDetail(packageName, epochDay)
            } else {
                Report
            }
        }
    }
}
