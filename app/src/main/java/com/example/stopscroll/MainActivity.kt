package com.example.stopscroll

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.stopscroll.ui.theme.CharcoalBlack
import com.example.stopscroll.ui.theme.StopScrollTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    val hasQuizAnswers = !selectedTimeWaster.isNullOrBlank() && !selectedWasteEstimate.isNullOrBlank()
    val permissionsMissing = arePermissionsMissing(context)

    var stage by rememberSaveable {
        mutableStateOf(AppEntryStage.INTRO)
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
                onFinish = {
                    stage = when {
                        !hasQuizAnswers -> AppEntryStage.QUIZ_STEP_ONE
                        permissionsMissing -> AppEntryStage.ACCESS_GATE
                        else -> AppEntryStage.DASHBOARD
                    }
                }
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

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
private fun StopScrollPreview() {
    StopScrollTheme {
        StopScrollApp()
    }
}
