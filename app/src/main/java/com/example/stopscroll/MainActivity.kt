package com.example.stopscroll

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.core.content.pm.PackageInfoCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stopscroll.ui.theme.AccentOrange
import com.example.stopscroll.R
import com.example.stopscroll.ui.theme.CharcoalBlack
import com.example.stopscroll.ui.theme.ElectricBlue
import com.example.stopscroll.ui.theme.ElectricBlueSoft
import com.example.stopscroll.ui.theme.NeonPink
import com.example.stopscroll.ui.theme.PositiveGreen
import com.example.stopscroll.ui.theme.StopScrollTheme
import com.example.stopscroll.ui.theme.SurfaceBlack
import com.example.stopscroll.ui.theme.SurfaceBlackSoft
import com.example.stopscroll.ui.theme.TextPrimary
import com.example.stopscroll.ui.theme.TextSecondary
import com.example.stopscroll.ui.theme.WarningRed
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val icon: String,
    val color: Color
)

data class InsightRowData(
    val label: String,
    val blockedAttempts: Int,
    val usedMinutes: Int
)

const val PREFS_NAME = "stopscroll_prefs"
const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
const val KEY_TIME_WASTER = "time_waster"
const val KEY_WASTE_ESTIMATE = "waste_estimate"
const val KEY_CHEAT_DAYS = "cheat_days"
const val KEY_BREAK_END_EPOCH_MS = "break_end_epoch_ms"
const val KEY_TOTAL_BLOCKED_ATTEMPTS = "total_blocked_attempts"
const val KEY_TOTAL_USED_SECONDS = "total_used_seconds"
const val KEY_DAY_PREFIX = "day_"
const val KEY_WEEK_PREFIX = "week_"
const val KEY_MONTH_PREFIX = "month_"
const val KEY_YEAR_PREFIX = "year_"
const val KEY_PLATFORM_INSTAGRAM = "Instagram"
const val KEY_PLATFORM_YOUTUBE = "YouTube"
const val KEY_PLATFORM_FACEBOOK = "Facebook"
const val KEY_PLATFORM_UNKNOWN = "Other"
const val KEY_BLOCK_INSTAGRAM = "block_instagram"
const val KEY_BLOCK_YOUTUBE = "block_youtube"
const val KEY_BLOCK_FACEBOOK = "block_facebook"
const val PACKAGE_INSTAGRAM = "com.instagram.android"
const val PACKAGE_INSTAGRAM_LITE = "com.instagram.lite"
const val PACKAGE_YOUTUBE = "com.google.android.youtube"
const val PACKAGE_YOUTUBE_REVANCED = "app.revanced.android.youtube"
const val PACKAGE_FACEBOOK = "com.facebook.katana"
const val PACKAGE_FACEBOOK_LITE = "com.facebook.lite"

private val TRACKED_PLATFORMS = setOf(
    KEY_PLATFORM_INSTAGRAM,
    KEY_PLATFORM_YOUTUBE,
    KEY_PLATFORM_FACEBOOK,
    KEY_PLATFORM_UNKNOWN
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StopScrollTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CharcoalBlack
                ) {
                    StopScrollApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun StopScrollApp() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var selectedTimeWaster by rememberSaveable {
        mutableStateOf(prefs.getString(KEY_TIME_WASTER, null))
    }
    var selectedWasteEstimate by rememberSaveable {
        mutableStateOf(prefs.getString(KEY_WASTE_ESTIMATE, null))
    }

    val onboardingCompleted = remember {
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    val hasQuizAnswers = !selectedTimeWaster.isNullOrBlank() && !selectedWasteEstimate.isNullOrBlank()

    var stage by rememberSaveable {
        mutableStateOf(
            when {
                onboardingCompleted -> AppEntryStage.DASHBOARD
                hasQuizAnswers -> AppEntryStage.ACCESS_GATE
                else -> AppEntryStage.INTRO
            }
        )
    }

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
        },
        label = "app-stage"
    ) { currentStage ->
        when (currentStage) {
            AppEntryStage.INTRO -> IntroAnimationScreen(
                onFinish = { stage = AppEntryStage.QUIZ_STEP_ONE }
            )

            AppEntryStage.QUIZ_STEP_ONE,
            AppEntryStage.QUIZ_STEP_TWO -> DiscoveryQuizFlow(
                step = if (currentStage == AppEntryStage.QUIZ_STEP_ONE) 1 else 2,
                selectedTimeWaster = selectedTimeWaster,
                selectedWasteEstimate = selectedWasteEstimate,
                onStepOneSelected = {
                    selectedTimeWaster = it
                    prefs.edit().putString(KEY_TIME_WASTER, it).apply()
                    stage = AppEntryStage.QUIZ_STEP_TWO
                },
                onStepTwoSelected = {
                    selectedWasteEstimate = it
                    prefs.edit().putString(KEY_WASTE_ESTIMATE, it).apply()
                    stage = AppEntryStage.ACCESS_GATE
                }
            )

            AppEntryStage.ACCESS_GATE -> AccessGateScreen(
                onUnlocked = {
                    prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
                    stage = AppEntryStage.PERMISSION_SUCCESS_ANIMATION
                }
            )

            AppEntryStage.PERMISSION_SUCCESS_ANIMATION -> PermissionSuccessAnimationScreen(
                onFinish = { stage = AppEntryStage.DASHBOARD }
            )

            AppEntryStage.DASHBOARD -> MainDashboardScreen(
                focusApp = selectedTimeWaster,
                wasteEstimate = selectedWasteEstimate
            )
        }
    }
}

@Composable
private fun IntroAnimationScreen(onFinish: () -> Unit) {
    var quoteVisible by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "brain-wave")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.84f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave-scale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave-alpha"
    )
    val quoteAlpha by animateFloatAsState(
        targetValue = if (quoteVisible) 1f else 0f,
        animationSpec = tween(650),
        label = "quote-fade"
    )

    LaunchedEffect(Unit) {
        delay(500)
        quoteVisible = true
        delay(1900)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrainWaveAnimation(
                modifier = Modifier.size(180.dp),
                scale = waveScale,
                alpha = waveAlpha
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Ready for Recovery...",
                style = TextStyle(
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.alpha(quoteAlpha)
            )
        }
    }
}

@Composable
private fun PermissionSuccessAnimationScreen(onFinish: () -> Unit) {
    var messageVisible by remember { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "permission-success")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "permission-success-scale"
    )

    LaunchedEffect(Unit) {
        delay(300)
        messageVisible = true
        delay(1300)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size((120f * pulseScale).dp)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = PositiveGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = messageVisible,
                enter = fadeIn(tween(260)),
                exit = fadeOut(tween(160))
            ) {
                Text(
                    text = "Permissions verified",
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                )
            }
        }
    }
}

@Composable
private fun BrainWaveAnimation(
    modifier: Modifier,
    scale: Float,
    alpha: Float
) {
    Canvas(modifier = modifier) {
        val minDimension = size.minDimension
        val radius = (minDimension * 0.24f) * scale
        drawCircle(
            color = ElectricBlue.copy(alpha = alpha * 0.3f),
            radius = radius * 1.95f,
            style = Stroke(width = minDimension * 0.065f)
        )
        drawCircle(
            color = NeonPink.copy(alpha = alpha * 0.25f),
            radius = radius * 1.48f,
            style = Stroke(width = minDimension * 0.055f)
        )
        drawCircle(
            color = ElectricBlueSoft.copy(alpha = alpha * 0.85f),
            radius = radius,
            style = Stroke(width = minDimension * 0.075f)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun DiscoveryQuizFlow(
    step: Int,
    selectedTimeWaster: String?,
    selectedWasteEstimate: String?,
    onStepOneSelected: (String) -> Unit,
    onStepTwoSelected: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally(animationSpec = tween(320)) { fullWidth -> fullWidth } +
                    fadeIn(animationSpec = tween(320)) togetherWith
                    slideOutHorizontally(animationSpec = tween(320)) { fullWidth -> -fullWidth / 2 } +
                    fadeOut(animationSpec = tween(180))
            },
            label = "quiz-steps"
        ) { activeStep ->
            when (activeStep) {
                1 -> QuizStepScreen(
                    step = 1,
                    totalSteps = 2,
                    question = "Which app is your top trigger? (blocking all enabled apps)",
                    options = listOf("Instagram", "YouTube", "Facebook", "Shorts Mix"),
                    selectedOption = selectedTimeWaster,
                    onOptionSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStepOneSelected(it)
                    }
                )

                else -> QuizStepScreen(
                    step = 2,
                    totalSteps = 2,
                    question = "How much time gets wasted daily?",
                    options = listOf("< 30 min", "30-60 min", "1-2 hours", "2+ hours"),
                    selectedOption = selectedWasteEstimate,
                    onOptionSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStepTwoSelected(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuizStepScreen(
    step: Int,
    totalSteps: Int,
    question: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { step.toFloat() / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = ElectricBlue,
            trackColor = SurfaceBlackSoft
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Step $step/$totalSteps",
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = question,
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(28.dp))

        options.forEach { option ->
            val selected = selectedOption == option
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) ElectricBlue.copy(alpha = 0.2f) else SurfaceBlack
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch {
                            onOptionSelected(option)
                            delay(120)
                        }
                    }
            ) {
                Text(
                    text = option,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun AccessGateScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }
    var showWarning by rememberSaveable { mutableStateOf(false) }

    var overlayGranted by remember { mutableStateOf(isOverlayPermissionGranted(context)) }
    var accessibilityGranted by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, StopScrollAccessibilityService::class.java))
    }
    var usageAccessGranted by remember { mutableStateOf(isUsageAccessGranted(context)) }
    var batteryIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            overlayGranted = isOverlayPermissionGranted(context)
            accessibilityGranted =
                isAccessibilityServiceEnabled(context, StopScrollAccessibilityService::class.java)
            usageAccessGranted = isUsageAccessGranted(context)
            batteryIgnored = isBatteryOptimizationIgnored(context)
            delay(800)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Hard Restriction Access Gate",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Accessibility and Overlay permission ke bina blocker activate nahi hoga.",
                color = TextSecondary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            PermissionStatusCard(
                title = "Accessibility Permission",
                granted = accessibilityGranted,
                actionText = "Open Accessibility",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    openAccessibilitySettings(context)
                }
            )

            PermissionStatusCard(
                title = "Overlay Permission",
                granted = overlayGranted,
                actionText = "Allow Overlay",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    openOverlaySettings(context)
                }
            )

            PermissionStatusCard(
                title = "Usage Access Permission",
                granted = usageAccessGranted,
                actionText = "Grant Usage Access",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    openUsageAccessSettings(context)
                }
            )

            PermissionStatusCard(
                title = "Battery Optimization",
                granted = batteryIgnored,
                actionText = "Whitelist App",
                isRequired = false,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    openBatteryOptimizationSettings(context)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (showWarning) {
                Text(
                    text = "Required permissions missing. Continue locked.",
                    color = WarningRed,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    if (accessibilityGranted && overlayGranted && usageAccessGranted) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUnlocked()
                    } else {
                        showWarning = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            shakeOffset.snapTo(0f)
                            repeat(4) {
                                shakeOffset.animateTo(12f, tween(55))
                                shakeOffset.animateTo(-12f, tween(55))
                            }
                            shakeOffset.animateTo(0f, tween(45))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .offset(x = shakeOffset.value.dp)
            ) {
                Text(
                    text = "Continue",
                    color = CharcoalBlack,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    granted: Boolean,
    actionText: String,
    isRequired: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = if (granted) "Granted" else if (isRequired) "Required" else "Recommended",
                        color = if (granted) PositiveGreen else AccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlackSoft),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = actionText,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MainDashboardScreen(
    focusApp: String?,
    wasteEstimate: String?
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var selectedTab by rememberSaveable { mutableStateOf(DashboardTab.SHIELD) }
    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var cheatDays by rememberSaveable { mutableStateOf(readCheatDays(prefs)) }
    var breakEndEpochMs by rememberSaveable { mutableStateOf(prefs.getLong(KEY_BREAK_END_EPOCH_MS, 0L)) }
    var previousBreakVisible by rememberSaveable {
        mutableStateOf(breakEndEpochMs > System.currentTimeMillis())
    }

    val platforms = remember {
        listOf(
            PlatformMeta(KEY_PLATFORM_INSTAGRAM, "📸", NeonPink),
            PlatformMeta(KEY_PLATFORM_YOUTUBE, "▶️", WarningRed),
            PlatformMeta(KEY_PLATFORM_FACEBOOK, "f", ElectricBlue)
        )
    }

    val platformEnabled = remember {
        mutableStateMapOf(
            KEY_PLATFORM_INSTAGRAM to prefs.getBoolean(KEY_BLOCK_INSTAGRAM, true),
            KEY_PLATFORM_YOUTUBE to prefs.getBoolean(KEY_BLOCK_YOUTUBE, true),
            KEY_PLATFORM_FACEBOOK to prefs.getBoolean(KEY_BLOCK_FACEBOOK, true)
        )
    }

    var dashboardState by remember {
        mutableStateOf(readDashboardRealtimeState(context, prefs))
    }

    LaunchedEffect(Unit) {
        delay(1200)
        releaseInfo = withContext(Dispatchers.IO) {
            checkForGithubReleaseUpdate(context)
        }
        showUpdateDialog = releaseInfo != null
    }

    LaunchedEffect(Unit) {
        while (true) {
            dashboardState = readDashboardRealtimeState(context, prefs)
            breakEndEpochMs = prefs.getLong(KEY_BREAK_END_EPOCH_MS, 0L)
            delay(1000)
        }
    }

    val breakSecondsRemaining =
        (((breakEndEpochMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)).toInt()

    Scaffold(
        containerColor = CharcoalBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = SurfaceBlack) {
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.SHIELD,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = DashboardTab.SHIELD
                    },
                    icon = {
                        Text(
                            text = "🛡️",
                            fontSize = 20.sp
                        )
                    },
                    label = {
                        Text(
                            text = "Shield",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.INSIGHTS,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = DashboardTab.INSIGHTS
                    },
                    icon = {
                        Text(
                            text = "📊",
                            fontSize = 20.sp
                        )
                    },
                    label = {
                        Text(
                            text = "Insights",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CharcoalBlack)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
            ) {
                AnimatedVisibility(
                    visible = releaseInfo != null,
                    enter = slideInVertically(tween(280)) + fadeIn(tween(280)),
                    exit = slideOutVertically(tween(180)) + fadeOut(tween(180))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceBlackSoft),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = releaseInfo?.let {
                                    "New update: ${it.latestVersionName}"
                                } ?: "New update detected",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Download APK",
                                color = ElectricBlue,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showUpdateDialog = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    DashboardTab.SHIELD -> ShieldSection(
                        totalWastedSeconds = dashboardState.usedSeconds,
                        blockedAttempts = dashboardState.blockedAttempts,
                        focusApp = focusApp,
                        wasteEstimate = wasteEstimate,
                        platforms = platforms,
                        platformEnabled = platformEnabled,
                        platformUsageSeconds = dashboardState.platformUsedSeconds,
                        breakSecondsRemaining = breakSecondsRemaining,
                        cheatDays = cheatDays,
                        onToggleChanged = { key, enabled ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            platformEnabled[key] = enabled
                            savePlatformToggle(prefs, key, enabled)
                        },
                        onBreakOptionSelected = { minutes ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val endEpoch = if (minutes <= 0) 0L else {
                                System.currentTimeMillis() + (minutes * 60_000L)
                            }
                            breakEndEpochMs = endEpoch
                            prefs.edit().putLong(KEY_BREAK_END_EPOCH_MS, endEpoch).apply()
                        },
                        onCheatDayToggle = { day ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            cheatDays = if (cheatDays.contains(day)) {
                                cheatDays - day
                            } else {
                                cheatDays + day
                            }
                            saveCheatDays(prefs, cheatDays)
                        }
                    )

                    DashboardTab.INSIGHTS -> InsightsSection(context = context, prefs = prefs)
                }
            }

            AnimatedVisibility(
                visible = breakSecondsRemaining > 0,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp),
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(999.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBlackSoft),
                    modifier = Modifier.shadow(8.dp, CircleShape)
                ) {
                    Text(
                        text = "Break Active ${formatTimer(breakSecondsRemaining)}",
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }

            if (showUpdateDialog && releaseInfo != null) {
                AlertDialog(
                    onDismissRequest = { showUpdateDialog = false },
                    containerColor = SurfaceBlack,
                    title = {
                        Text(
                            text = "Update Available",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    text = {
                        Text(
                            text = "Version ${releaseInfo?.latestVersionName} is ready. Download new APK from GitHub release.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showUpdateDialog = false
                                openApkDownload(context, releaseInfo?.apkUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Text(
                                text = "Download",
                                color = CharcoalBlack,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUpdateDialog = false }) {
                            Text(
                                text = "Later",
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(breakSecondsRemaining) {
        val currentlyVisible = breakSecondsRemaining > 0
        if (previousBreakVisible && !currentlyVisible) {
            prefs.edit().putLong(KEY_BREAK_END_EPOCH_MS, 0L).apply()
            breakEndEpochMs = 0L
            scope.launch {
                snackbarHostState.showSnackbar("Blocker resumed")
            }
        }
        previousBreakVisible = currentlyVisible
    }
}

@Composable
private fun ShieldSection(
    totalWastedSeconds: Int,
    blockedAttempts: Int,
    focusApp: String?,
    wasteEstimate: String?,
    platforms: List<PlatformMeta>,
    platformEnabled: Map<String, Boolean>,
    platformUsageSeconds: Map<String, Int>,
    breakSecondsRemaining: Int,
    cheatDays: Set<String>,
    onToggleChanged: (String, Boolean) -> Unit,
    onBreakOptionSelected: (Int) -> Unit,
    onCheatDayToggle: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.stopscroll_logo),
                        contentDescription = "StopScroll logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                    Column {
                        Text(
                            text = "Stopscroll",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp
                        )
                        Text(
                            text = "by Odlix",
                            color = ElectricBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                if (!focusApp.isNullOrBlank() || !wasteEstimate.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Focus target: ${focusApp ?: "--"} • Estimate: ${wasteEstimate ?: "--"}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Today's blocked attempts: $blockedAttempts",
                    color = WarningRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            if (totalWastedSeconds == 0) {
                SuccessVisualState()
            } else {
                ActiveTrackingVisualState(
                    totalWastedSeconds = totalWastedSeconds,
                    platforms = platforms,
                    platformEnabled = platformEnabled,
                    platformUsageSeconds = platformUsageSeconds
                )
            }
        }

        item {
            Text(
                text = "Platform Toggles",
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Reels/shorts feed detected hone par selected platforms auto-block honge.",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        items(platforms) { platform ->
            val checked = platformEnabled[platform.label] == true
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(platform.color.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = platform.icon,
                                color = platform.color,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = platform.label,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (checked) "Blocking ON" else "Blocking OFF",
                                color = if (checked) PositiveGreen else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = { onToggleChanged(platform.label, it) }
                    )
                }
            }
        }

        item {
            Text(
                text = "Quick Break",
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onBreakOptionSelected(0) },
                    colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Active",
                        color = CharcoalBlack,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
                Button(
                    onClick = { onBreakOptionSelected(5) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "5 min time waste",
                        color = CharcoalBlack,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { onBreakOptionSelected(10) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlueSoft),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "10 min time waste",
                        color = CharcoalBlack,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "Cheat Day Selector",
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        }

        item {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { day ->
                    FilterChip(
                        selected = cheatDays.contains(day),
                        onClick = { onCheatDayToggle(day) },
                        label = {
                            Text(
                                text = day,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue,
                            selectedLabelColor = CharcoalBlack,
                            containerColor = SurfaceBlack,
                            labelColor = TextPrimary
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(if (breakSecondsRemaining > 0) 82.dp else 18.dp))
        }
    }
}

@Composable
private fun SuccessVisualState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "👨‍👩‍👧‍👦",
                fontSize = 52.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "You didn't scroll today 🥳",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun ActiveTrackingVisualState(
    totalWastedSeconds: Int,
    platforms: List<PlatformMeta>,
    platformEnabled: Map<String, Boolean>,
    platformUsageSeconds: Map<String, Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SegmentedProgressRing(
                platforms = platforms,
                platformEnabled = platformEnabled,
                platformUsageSeconds = platformUsageSeconds,
                totalWastedSeconds = totalWastedSeconds
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Scrolling detected",
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SegmentedProgressRing(
    platforms: List<PlatformMeta>,
    platformEnabled: Map<String, Boolean>,
    platformUsageSeconds: Map<String, Int>,
    totalWastedSeconds: Int
) {
    val totalEnabledUsage = platforms.sumOf {
        if (platformEnabled[it.label] == true) platformUsageSeconds[it.label] ?: 0 else 0
    }

    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(216.dp)) {
            val strokeWidth = 19f
            val arcSize = Size(size.width, size.height)
            val gap = 8f
            var startAngle = -90f

            platforms.forEach { platform ->
                val usage = if (platformEnabled[platform.label] == true) {
                    platformUsageSeconds[platform.label] ?: 0
                } else {
                    0
                }
                val sweep = if (totalEnabledUsage > 0) {
                    (usage.toFloat() / totalEnabledUsage.toFloat()) * (360f - gap * platforms.size)
                } else {
                    0f
                }

                if (sweep > 0f) {
                    drawArc(
                        color = platform.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = arcSize
                    )
                }
                startAngle += sweep + gap
            }
        }

        val ringRadiusPx = 102.dp.value
        platforms.forEachIndexed { index, platform ->
            val angle = ((index * (360f / platforms.size)) - 90f) * (PI.toFloat() / 180f)
            val xOffset = (cos(angle) * ringRadiusPx).roundToInt()
            val yOffset = (sin(angle) * ringRadiusPx).roundToInt()

            Text(
                text = platform.icon,
                color = platform.color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (platform.icon == "f") 26.sp else 22.sp,
                modifier = Modifier
                    .offset { IntOffset(xOffset, yOffset) }
                    .shadow(10.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatDuration(totalWastedSeconds),
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp
            )
            Text(
                text = "wasted",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InsightsSection(context: Context, prefs: SharedPreferences) {
    var selectedFilter by rememberSaveable { mutableStateOf(InsightFilter.DAILY) }
    var rows by remember {
        mutableStateOf(loadInsightRows(context, prefs, selectedFilter))
    }

    LaunchedEffect(selectedFilter) {
        rows = loadInsightRows(context, prefs, selectedFilter)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Recovery Insights",
            color = TextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 34.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        val filterList = InsightFilter.entries
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filterList) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricBlue,
                        selectedLabelColor = CharcoalBlack,
                        containerColor = SurfaceBlack,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rows) { row ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = row.label,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Blocked Attempts: ${row.blockedAttempts}",
                                color = WarningRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Used Minutes: ${row.usedMinutes}",
                                color = PositiveGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}m ${seconds}s"
}

private fun formatTimer(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun isOverlayPermissionGranted(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun isAccessibilityServiceEnabled(
    context: Context,
    serviceClass: Class<out AccessibilityService>
): Boolean {
    val expectedService = ComponentName(context, serviceClass).flattenToString()
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServices.split(':').any {
        it.equals(expectedService, ignoreCase = true)
    }
}

private fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openBatteryOptimizationSettings(context: Context) {
    val requestIntent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(requestIntent)
    } catch (_: ActivityNotFoundException) {
        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallbackIntent)
    }
}

private fun openUsageAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun openApkDownload(context: Context, overrideUrl: String? = null) {
    val targetUrl = if (overrideUrl.isNullOrBlank()) {
        BuildConfig.FALLBACK_APK_URL
    } else {
        overrideUrl
    }

    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(targetUrl)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun mapPackageToPlatform(packageName: String): String {
    return when (packageName) {
        PACKAGE_INSTAGRAM,
        PACKAGE_INSTAGRAM_LITE -> KEY_PLATFORM_INSTAGRAM

        PACKAGE_YOUTUBE,
        PACKAGE_YOUTUBE_REVANCED -> KEY_PLATFORM_YOUTUBE

        PACKAGE_FACEBOOK,
        PACKAGE_FACEBOOK_LITE -> KEY_PLATFORM_FACEBOOK

        else -> KEY_PLATFORM_UNKNOWN
    }
}

fun savePlatformToggle(prefs: SharedPreferences, key: String, enabled: Boolean) {
    val targetKey = when (key) {
        KEY_PLATFORM_INSTAGRAM -> KEY_BLOCK_INSTAGRAM
        KEY_PLATFORM_YOUTUBE -> KEY_BLOCK_YOUTUBE
        KEY_PLATFORM_FACEBOOK -> KEY_BLOCK_FACEBOOK
        else -> null
    }

    targetKey?.let {
        prefs.edit().putBoolean(it, enabled).apply()
    }
}

fun readCheatDays(prefs: SharedPreferences): Set<String> {
    val serialized = prefs.getString(KEY_CHEAT_DAYS, "") ?: ""
    if (serialized.isBlank()) {
        return emptySet()
    }
    return serialized.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
}

fun saveCheatDays(prefs: SharedPreferences, days: Set<String>) {
    val serialized = days.sorted().joinToString(",")
    prefs.edit().putString(KEY_CHEAT_DAYS, serialized).apply()
}

fun readDashboardRealtimeState(context: Context, prefs: SharedPreferences): DashboardRealtimeState {
    ensureCurrentPeriodCounters(context, prefs)

    val todayDateKey = getDateKey(0)
    val platformMap = mutableMapOf<String, Int>()
    TRACKED_PLATFORMS.forEach { platform ->
        if (platform != KEY_PLATFORM_UNKNOWN) {
            val seconds = prefs.getInt(getDayPlatformSecondsKey(platform), 0)
            platformMap[platform] = seconds
        }
    }

    val blockedAttempts = prefs.getInt("${KEY_DAY_PREFIX}${todayDateKey}_blocked", 0)
    val usedSeconds = prefs.getInt("${KEY_DAY_PREFIX}${todayDateKey}_used", 0)

    return DashboardRealtimeState(
        platformUsedSeconds = platformMap,
        blockedAttempts = blockedAttempts,
        usedSeconds = usedSeconds
    )
}

fun loadInsightRows(
    context: Context,
    prefs: SharedPreferences,
    filter: InsightFilter
): List<InsightRowData> {
    ensureCurrentPeriodCounters(context, prefs)
    return when (filter) {
        InsightFilter.DAILY -> {
            val labels = listOf("Today", "Yesterday", "2 days ago", "3 days ago", "4 days ago", "5 days ago", "6 days ago")
            labels.mapIndexed { index, label ->
                val keyDate = getDateKey(daysAgo = index)
                val blocked = prefs.getInt("${KEY_DAY_PREFIX}${keyDate}_blocked", 0)
                val usedSeconds = prefs.getInt("${KEY_DAY_PREFIX}${keyDate}_used", 0)
                InsightRowData(label, blocked, usedSeconds / 60)
            }
        }

        InsightFilter.WEEKLY -> {
            val weekBuckets = LinkedHashMap<String, Pair<Int, Int>>()
            (0..55).forEach { dayOffset ->
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
                val keyDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(calendar.time)
                val weekKey = "${calendar.get(Calendar.YEAR)}-W${calendar.get(Calendar.WEEK_OF_YEAR)}"

                val used = prefs.getInt("${KEY_DAY_PREFIX}${keyDate}_used", 0)
                val blocked = prefs.getInt("${KEY_DAY_PREFIX}${keyDate}_blocked", 0)
                val current = weekBuckets[weekKey] ?: (0 to 0)
                weekBuckets[weekKey] = Pair(current.first + blocked, current.second + used)
            }
            weekBuckets.entries.take(8).map { (weekKey, value) ->
                InsightRowData("Week $weekKey", value.first, value.second / 60)
            }
        }

        InsightFilter.MONTHLY -> {
            val monthBuckets = LinkedHashMap<String, Pair<Int, Int>>()
            (0..364).forEach { dayOffset ->
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
                val keyDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(calendar.time)
                val monthKey = SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time)

                val used = prefs.getInt("${KEY_DAY_PREFIX}${keyDate}_used", 0)
                val blocked = prefs.getInt("${KEY_DAY_PREFIX}${keyDate}_blocked", 0)
                val current = monthBuckets[monthKey] ?: (0 to 0)
                monthBuckets[monthKey] = Pair(current.first + blocked, current.second + used)
            }
            monthBuckets.entries.take(12).map { (monthKey, value) ->
                InsightRowData(monthKey, value.first, value.second / 60)
            }
        }

        InsightFilter.YEARLY -> {
            val yearBuckets = LinkedHashMap<String, Pair<Int, Int>>()
            (0..1825).forEach { dayOffset ->
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
                val keyDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(calendar.time)
                val yearKey = calendar.get(Calendar.YEAR).toString()

                val used = prefs.getInt("${KEY_DAY_PREFIX}${keyDate}_used", 0)
                val blocked = prefs.getInt("${KEY_DAY_PREFIX}${keyDate}_blocked", 0)
                val current = yearBuckets[yearKey] ?: (0 to 0)
                yearBuckets[yearKey] = Pair(current.first + blocked, current.second + used)
            }
            yearBuckets.entries.take(5).map { (yearKey, value) ->
                InsightRowData(yearKey, value.first, value.second / 60)
            }
        }
    }.filter { it.blockedAttempts > 0 || it.usedMinutes > 0 }
}

fun getDateKey(daysAgo: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return SimpleDateFormat("yyyyMMdd", Locale.US).format(calendar.time)
}

fun getDayPlatformSecondsKey(platform: String): String {
    return "${KEY_DAY_PREFIX}${getDateKey(0)}_${platform.lowercase(Locale.US)}_seconds"
}

fun isUsageAccessGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun queryTodayUsageSeconds(context: Context): Map<String, Int> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val start = calendar.timeInMillis
    val end = System.currentTimeMillis()
    val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        start,
        end
    )

    val map = mutableMapOf<String, Int>()
    stats.forEach { usage ->
        val platform = mapPackageToPlatform(usage.packageName)
        if (platform != KEY_PLATFORM_UNKNOWN) {
            val seconds = (usage.totalTimeInForeground / 1000L).toInt()
            map[platform] = (map[platform] ?: 0) + seconds
        }
    }
    return map
}

fun ensureCurrentPeriodCounters(context: Context, prefs: SharedPreferences) {
    val usage = if (isUsageAccessGranted(context)) {
        queryTodayUsageSeconds(context)
    } else {
        emptyMap()
    }

    val dayKey = getDateKey(0)

    val editor = prefs.edit()
    val todayUsedSeconds = usage.values.sum()
    editor.putInt(KEY_TOTAL_USED_SECONDS, todayUsedSeconds)
    editor.putInt("${KEY_DAY_PREFIX}${dayKey}_used", todayUsedSeconds)

    TRACKED_PLATFORMS.forEach { platform ->
        if (platform != KEY_PLATFORM_UNKNOWN) {
            editor.putInt(getDayPlatformSecondsKey(platform), usage[platform] ?: 0)
        }
    }
    editor.apply()

    val dayPrefix = "${KEY_DAY_PREFIX}"
    val allDayUsedKeys = prefs.all.keys.filter {
        it.startsWith(dayPrefix) && it.endsWith("_used") && !it.contains("_seconds")
    }

    val weekBuckets = mutableMapOf<String, Pair<Int, Int>>()
    val monthBuckets = mutableMapOf<String, Pair<Int, Int>>()
    val yearBuckets = mutableMapOf<String, Pair<Int, Int>>()

    allDayUsedKeys.forEach { dayUsedKey ->
        val dateToken = dayUsedKey.removePrefix(dayPrefix).removeSuffix("_used")
        if (dateToken.length == 8) {
            val parsed = SimpleDateFormat("yyyyMMdd", Locale.US).parse(dateToken)
            if (parsed != null) {
                val calendar = Calendar.getInstance()
                calendar.time = parsed

                val weekKey = "${calendar.get(Calendar.YEAR)}-W${calendar.get(Calendar.WEEK_OF_YEAR)}"
                val monthKey = SimpleDateFormat("MMMM yyyy", Locale.US).format(parsed)
                val yearKey = calendar.get(Calendar.YEAR).toString()

                val used = prefs.getInt("${KEY_DAY_PREFIX}${dateToken}_used", 0)
                val blocked = prefs.getInt("${KEY_DAY_PREFIX}${dateToken}_blocked", 0)

                val weekCurrent = weekBuckets[weekKey] ?: (0 to 0)
                weekBuckets[weekKey] = Pair(weekCurrent.first + blocked, weekCurrent.second + used)

                val monthCurrent = monthBuckets[monthKey] ?: (0 to 0)
                monthBuckets[monthKey] = Pair(monthCurrent.first + blocked, monthCurrent.second + used)

                val yearCurrent = yearBuckets[yearKey] ?: (0 to 0)
                yearBuckets[yearKey] = Pair(yearCurrent.first + blocked, yearCurrent.second + used)
            }
        }
    }

    val aggregateEditor = prefs.edit()
    weekBuckets.forEach { (key, value) ->
        aggregateEditor.putInt("${KEY_WEEK_PREFIX}${key}_blocked", value.first)
        aggregateEditor.putInt("${KEY_WEEK_PREFIX}${key}_used", value.second)
    }
    monthBuckets.forEach { (key, value) ->
        aggregateEditor.putInt("${KEY_MONTH_PREFIX}${key}_blocked", value.first)
        aggregateEditor.putInt("${KEY_MONTH_PREFIX}${key}_used", value.second)
    }
    yearBuckets.forEach { (key, value) ->
        aggregateEditor.putInt("${KEY_YEAR_PREFIX}${key}_blocked", value.first)
        aggregateEditor.putInt("${KEY_YEAR_PREFIX}${key}_used", value.second)
    }
    aggregateEditor.apply()
}

fun checkForGithubReleaseUpdate(context: Context): ReleaseInfo? {
    if (BuildConfig.GITHUB_RELEASES_API_URL.contains("REPLACE_OWNER")) {
        return null
    }

    return try {
        val connection = URL(BuildConfig.GITHUB_RELEASES_API_URL).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("Accept", "application/vnd.github+json")

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            connection.disconnect()
            return null
        }

        val body = connection.inputStream.bufferedReader().use { reader ->
            reader.readText()
        }
        connection.disconnect()

        val tagName = extractJsonString(body, "tag_name") ?: return null
        val versionName = tagName.removePrefix("v")
        val latestVersionCode = parseVersionCodeFromTag(versionName)

        val apkUrl = extractApkUrlFromReleaseBody(body) ?: BuildConfig.FALLBACK_APK_URL
        val currentCode = PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0)
        ).toInt()

        if (latestVersionCode > currentCode) {
            ReleaseInfo(
                latestVersionCode = latestVersionCode,
                latestVersionName = versionName,
                apkUrl = apkUrl
            )
        } else {
            null
        }
    } catch (exception: Exception) {
        Log.w("StopScrollUpdate", "Update check failed", exception)
        null
    }
}

fun parseVersionCodeFromTag(versionName: String): Int {
    val pieces = versionName.split('.')
    if (pieces.isEmpty()) {
        return 0
    }

    val major = pieces.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = pieces.getOrNull(1)?.toIntOrNull() ?: 0
    val patch = pieces.getOrNull(2)?.toIntOrNull() ?: 0
    return (major * 10000) + (minor * 100) + patch
}

fun extractJsonString(body: String, fieldName: String): String? {
    val key = "\"$fieldName\""
    val keyIndex = body.indexOf(key)
    if (keyIndex < 0) {
        return null
    }

    val colonIndex = body.indexOf(':', keyIndex)
    if (colonIndex < 0) {
        return null
    }

    val firstQuote = body.indexOf('"', colonIndex + 1)
    if (firstQuote < 0) {
        return null
    }

    var cursor = firstQuote + 1
    var escaped = false
    val output = StringBuilder()

    while (cursor < body.length) {
        val ch = body[cursor]
        if (escaped) {
            output.append(ch)
            escaped = false
        } else if (ch == '\\') {
            escaped = true
        } else if (ch == '"') {
            return output.toString()
        } else {
            output.append(ch)
        }
        cursor += 1
    }

    return null
}

fun extractApkUrlFromReleaseBody(body: String): String? {
    val downloadKey = "browser_download_url"
    var cursor = 0

    while (true) {
        val keyIndex = body.indexOf(downloadKey, cursor)
        if (keyIndex < 0) {
            break
        }

        val colonIndex = body.indexOf(':', keyIndex)
        if (colonIndex < 0) {
            break
        }

        val firstQuote = body.indexOf('"', colonIndex + 1)
        if (firstQuote < 0) {
            break
        }

        var endQuote = firstQuote + 1
        var escaped = false
        while (endQuote < body.length) {
            val current = body[endQuote]
            if (!escaped && current == '"') {
                break
            }
            escaped = (!escaped && current == '\\')
            if (current != '\\') {
                escaped = false
            }
            endQuote += 1
        }

        if (endQuote >= body.length) {
            break
        }

        val url = body.substring(firstQuote + 1, endQuote).replace("\\/", "/")
        if (url.lowercase(Locale.US).endsWith(".apk")) {
            return url
        }

        cursor = endQuote + 1
    }

    return null
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
private fun StopScrollPreview() {
    StopScrollTheme {
        StopScrollApp()
    }
}
