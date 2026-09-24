package com.anandrajbgp.stopscroll

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedHashMap
import java.util.Locale

fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}m ${seconds}s"
}

fun arePermissionsMissing(context: Context): Boolean {
    val overlay = isOverlayPermissionGranted(context)
    val accessibility = isAccessibilityServiceEnabled(context, StopScrollAccessibilityService::class.java)
    val usage = isUsageAccessGranted(context)
    return !overlay || !accessibility || !usage
}

fun formatTimer(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun isOverlayPermissionGranted(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

fun isAccessibilityServiceEnabled(
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

fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun openBatteryOptimizationSettings(context: Context) {
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

fun openUsageAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun openApkDownload(context: Context, overrideUrl: String? = null) {
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
    if (
        BuildConfig.GITHUB_RELEASES_API_URL.isBlank() ||
        !BuildConfig.GITHUB_RELEASES_API_URL.contains("/releases/latest")
    ) {
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
