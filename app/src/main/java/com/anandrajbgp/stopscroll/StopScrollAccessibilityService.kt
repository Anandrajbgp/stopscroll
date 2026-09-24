package com.anandrajbgp.stopscroll

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StopScrollAccessibilityService : AccessibilityService() {

    private var lastBlockTimestampMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("StopScrollService", "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val safeEvent = event ?: return
        val packageName = safeEvent.packageName?.toString() ?: return
        val eventType = safeEvent.eventType

        if (
            eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) {
            return
        }

        val platform = mapPackageToPlatform(packageName)
        if (platform == KEY_PLATFORM_UNKNOWN) {
            return
        }

        if (!shouldBlockPlatform(platform)) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastBlockTimestampMs < 2000L) {
            return
        }

        val reelDetected = isReelLikeContext(safeEvent, platform)
        if (!reelDetected) {
            return
        }

        lastBlockTimestampMs = now
        registerBlockedAttempt(now, platform)

        val backBlocked = performGlobalAction(GLOBAL_ACTION_BACK)

        Log.d(
            "StopScrollService",
            "blockAttempt platform=$platform back=$backBlocked class=${safeEvent.className} text=${safeEvent.text}"
        )

        Toast.makeText(
            this,
            "Reels blocked on $platform",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onInterrupt() {
        // Required override for system interruptions.
    }

    private fun shouldBlockPlatform(platform: String): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        if (!prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)) {
            return false
        }

        val isBreakActive = prefs.getLong(KEY_BREAK_END_EPOCH_MS, 0L) > System.currentTimeMillis()
        if (isBreakActive) {
            return false
        }

        val cheatDays = readCheatDays(prefs)
        val currentDay = SimpleDateFormat("EEE", Locale.US).format(Calendar.getInstance().time)
        if (cheatDays.contains(currentDay)) {
            return false
        }

        return when (platform) {
            KEY_PLATFORM_INSTAGRAM -> prefs.getBoolean(KEY_BLOCK_INSTAGRAM, true)
            KEY_PLATFORM_YOUTUBE -> prefs.getBoolean(KEY_BLOCK_YOUTUBE, true)
            KEY_PLATFORM_FACEBOOK -> prefs.getBoolean(KEY_BLOCK_FACEBOOK, true)
            else -> false
        }
    }

    private fun isReelLikeContext(event: AccessibilityEvent, platform: String): Boolean {
        val className = event.className?.toString()?.lowercase(Locale.US).orEmpty()
        val textPayload = buildString {
            event.text.forEach { append(it.toString()).append(' ') }
            append(event.contentDescription?.toString().orEmpty())
            append(' ')
            append(className)
            append(' ')
            append(event.source?.viewIdResourceName.orEmpty())
        }.lowercase(Locale.US)

        val keywords = when (platform) {
            KEY_PLATFORM_INSTAGRAM -> listOf(
                "reel",
                "reels",
                "suggested reels",
                "reel_viewer",
                "clips"
            )

            KEY_PLATFORM_YOUTUBE -> listOf(
                "shorts",
                "short",
                "reel",
                "shorts_player"
            )

            KEY_PLATFORM_FACEBOOK -> listOf(
                "reels",
                "reel",
                "watch",
                "video_feed"
            )

            else -> emptyList()
        }

        val hasPlatformKeyword = keywords.any { keyword ->
            textPayload.contains(keyword)
        }

        val strictClassMatch = className.contains("reel") ||
            className.contains("short") ||
            className.contains("clips")

        return hasPlatformKeyword || strictClassMatch
    }

    private fun registerBlockedAttempt(nowMs: Long, platform: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val dayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(nowMs)

        val totalBlocked = prefs.getInt(KEY_TOTAL_BLOCKED_ATTEMPTS, 0) + 1
        val dayBlockedKey = "${KEY_DAY_PREFIX}${dayKey}_blocked"
        val dayBlocked = prefs.getInt(dayBlockedKey, 0) + 1

        val platformKey = "${KEY_DAY_PREFIX}${dayKey}_${platform.lowercase(Locale.US)}_blocked"
        val platformBlocked = prefs.getInt(platformKey, 0) + 1

        prefs.edit()
            .putInt(KEY_TOTAL_BLOCKED_ATTEMPTS, totalBlocked)
            .putInt(dayBlockedKey, dayBlocked)
            .putInt(platformKey, platformBlocked)
            .apply()
    }
}
