package com.anandrajbgp.stopscroll

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Facebook
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anandrajbgp.stopscroll.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun IntroAnimationScreen(onFinish: () -> Unit) {
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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.alpha(quoteAlpha)
            )
        }
    }
}

@Composable
fun BrainWaveAnimation(modifier: Modifier, scale: Float, alpha: Float) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = ElectricBlue,
                radius = (size.minDimension / 2.2f) * scale,
                alpha = 0.15f * alpha,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = ElectricBlue,
                radius = (size.minDimension / 3f) * scale,
                alpha = 0.3f * alpha,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Surface(
            modifier = Modifier
                .size(80.dp)
                .shadow(20.dp, CircleShape, spotColor = ElectricBlue),
            shape = CircleShape,
            color = SurfaceBlackSoft,
            border = BorderStroke(2.dp, ElectricBlue)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🧠", fontSize = 38.sp)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DiscoveryQuizFlow(
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
            .padding(horizontal = 20.dp, vertical = 40.dp)
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
fun QuizStepScreen(
    step: Int,
    totalSteps: Int,
    question: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(30.dp))
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
fun AccessGateScreen(onUnlocked: () -> Unit) {
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
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Access Gate",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "These permissions allow StopScroll to monitor and interrupt infinite scrolling apps. Your privacy is respected, and no personal data is collected or shared.",
                color = TextSecondary.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                    .height(50.dp)
                    .offset(x = shakeOffset.value.dp)
            ) {
                Text(
                    text = "Continue",
                    color = CharcoalBlack,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PermissionStatusCard(
    title: String,
    granted: Boolean,
    actionText: String,
    isRequired: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (granted) "Granted" else if (isRequired) "Required" else "Recommended",
                        color = if (granted) PositiveGreen else AccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onClick,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlackSoft),
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
fun PermissionSuccessAnimationScreen(onFinish: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "permission-success")
    val scale by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "permission-success-scale"
    )

    LaunchedEffect(Unit) {
        delay(2200)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    drawCircle(
                        color = PositiveGreen,
                        radius = (size.minDimension / 2f) * scale,
                        alpha = 0.2f
                    )
                }
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = PositiveGreen
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✅", fontSize = 40.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Permissions verified",
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
            Text(
                text = "You are ready to go!",
                color = TextSecondary,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun MainDashboardScreen(
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

    val platforms = remember {
        listOf(
            PlatformMeta(KEY_PLATFORM_INSTAGRAM, Icons.Rounded.CameraAlt, NeonPink),
            PlatformMeta(KEY_PLATFORM_YOUTUBE, Icons.Rounded.PlayCircle, WarningRed),
            PlatformMeta(KEY_PLATFORM_FACEBOOK, Icons.Rounded.Facebook, ElectricBlue)
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
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield",
                            modifier = Modifier.size(24.dp),
                            tint = if (selectedTab == DashboardTab.SHIELD) ElectricBlue else TextSecondary
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
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = "Insights",
                            modifier = Modifier.size(24.dp),
                            tint = if (selectedTab == DashboardTab.INSIGHTS) ElectricBlue else TextSecondary
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
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
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

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:team.odlix@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "stopscroll by odlix")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MailOutline,
                            contentDescription = "Feedback",
                            tint = ElectricBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

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
                            platformEnabled[key] = enabled
                            savePlatformToggle(prefs, key, enabled)
                        },
                        onBreakOptionSelected = { minutes ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val endMs = System.currentTimeMillis() + (minutes * 60 * 1000L)
                            prefs.edit().putLong(KEY_BREAK_END_EPOCH_MS, endMs).apply()
                            breakEndEpochMs = endMs
                        },
                        onCheatDayToggle = { day ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val newDays = if (cheatDays.contains(day)) {
                                cheatDays - day
                            } else {
                                cheatDays + day
                            }
                            cheatDays = newDays
                            saveCheatDays(prefs, newDays)
                        }
                    )

                    DashboardTab.INSIGHTS -> InsightsSection(context = context, prefs = prefs)
                }
            }
        }
    }

    if (showUpdateDialog && releaseInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Software Update") },
            text = {
                Text(
                    "A new version of StopScroll is available (${releaseInfo?.latestVersionName}). Would you like to download it now?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    openApkDownload(context, releaseInfo?.apkUrl)
                }) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Later")
                }
            },
            containerColor = SurfaceBlackSoft,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
fun ShieldSection(
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
                        .padding(16.dp),
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
                            Icon(
                                imageVector = platform.icon,
                                contentDescription = platform.label,
                                tint = platform.color,
                                modifier = Modifier.size(20.dp)
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
                                text = if (checked) "Shield Active" else "Shield Off",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Break",
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                if (breakSecondsRemaining > 0) {
                    Text(
                        text = "Break ends in: ${formatTimer(breakSecondsRemaining)}",
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
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
                        text = "5min waste",
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
                        text = "10min waste",
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
                    val isSelected = cheatDays.contains(day)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCheatDayToggle(day) },
                        label = { Text(day) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue,
                            selectedLabelColor = CharcoalBlack,
                            labelColor = TextSecondary,
                            containerColor = SurfaceBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun SuccessVisualState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawCircle(color = PositiveGreen, alpha = 0.1f)
                }
                Text("🌿", fontSize = 48.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Mindful State Active",
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            Text(
                text = "No scrolling triggers detected today.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActiveTrackingVisualState(
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
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                platforms.forEach { platform ->
                    val usage = platformUsageSeconds[platform.label] ?: 0
                    PlatformMiniStat(platform, usage, totalWastedSeconds)
                }
            }
        }
    }
}

@Composable
fun PlatformMiniStat(platform: PlatformMeta, seconds: Int, totalSeconds: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Icon(
            imageVector = platform.icon,
            contentDescription = platform.label,
            modifier = Modifier.size(22.dp),
            tint = platform.color
        )
        Text(
            text = formatDuration(seconds),
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (totalSeconds > 0) seconds.toFloat() / totalSeconds.toFloat() else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = platform.color,
            trackColor = platform.color.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun SegmentedProgressRing(
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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatDuration(totalWastedSeconds),
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp
            )
            Text(
                text = "WASTED",
                color = ElectricBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun InsightsSection(context: Context, prefs: SharedPreferences) {
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
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricBlue,
                        selectedLabelColor = CharcoalBlack,
                        containerColor = SurfaceBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (rows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No data yet. Keep recovering!",
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rows) { row ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = row.label,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Blocked Attempts: ${row.blockedAttempts}",
                                    color = AccentOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${row.usedMinutes} min",
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
