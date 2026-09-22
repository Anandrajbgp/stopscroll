package com.example.stopscroll

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppEntryStage {
    INTRO,
    QUIZ_STEP_ONE,
    QUIZ_STEP_TWO,
    ACCESS_GATE,
    PERMISSION_SUCCESS_ANIMATION,
    DASHBOARD
}

enum class DashboardTab {
    SHIELD,
    INSIGHTS
}

enum class InsightFilter {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class PlatformMeta(
    val label: String,
    val icon: ImageVector,
    val color: Color
)

data class InsightRowData(
    val label: String,
    val blockedAttempts: Int,
    val usedMinutes: Int
)

data class DashboardRealtimeState(
    val platformUsedSeconds: Map<String, Int>,
    val blockedAttempts: Int,
    val usedSeconds: Int
)

data class ReleaseInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkUrl: String
)
