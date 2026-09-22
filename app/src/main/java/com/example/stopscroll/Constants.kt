package com.example.stopscroll

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
const val KEY_PLATFORM_UNKNOWN = "Unknown"
const val KEY_BLOCK_INSTAGRAM = "block_instagram"
const val KEY_BLOCK_YOUTUBE = "block_youtube"
const val KEY_BLOCK_FACEBOOK = "block_facebook"
const val PACKAGE_INSTAGRAM = "com.instagram.android"
const val PACKAGE_INSTAGRAM_LITE = "com.instagram.lite"
const val PACKAGE_YOUTUBE = "com.google.android.youtube"
const val PACKAGE_YOUTUBE_REVANCED = "app.revanced.android.youtube"
const val PACKAGE_FACEBOOK = "com.facebook.katana"
const val PACKAGE_FACEBOOK_LITE = "com.facebook.lite"

val TRACKED_PLATFORMS = setOf(
    KEY_PLATFORM_INSTAGRAM,
    KEY_PLATFORM_YOUTUBE,
    KEY_PLATFORM_FACEBOOK,
    KEY_PLATFORM_UNKNOWN
)
