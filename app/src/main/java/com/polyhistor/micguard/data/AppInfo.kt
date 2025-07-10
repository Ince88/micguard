package com.polyhistor.micguard.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean = false,
    val isMonitored: Boolean = false,
    val hasMicrophonePermission: Boolean = false,
    val lastMicUsage: Long = 0L
) {
    val displayName: String
        get() = if (appName.isBlank()) packageName else appName
} 