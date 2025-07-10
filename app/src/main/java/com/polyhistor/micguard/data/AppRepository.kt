package com.polyhistor.micguard.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

class AppRepository(private val context: Context) {
    
    private val dataStore: DataStore<Preferences> = PreferencesDataStore.getInstance(context)
    
    private val monitoredAppsKey = stringSetPreferencesKey("monitored_apps")
    private val isMonitoringEnabledKey = booleanPreferencesKey("is_monitoring_enabled")
    private val monitoringModeKey = stringPreferencesKey("monitoring_mode")
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
    private val autoStartKey = booleanPreferencesKey("auto_start")
    private val isDarkThemeKey = booleanPreferencesKey("is_dark_theme")
    
    suspend fun getInstalledApps(): List<AppInfo> {
        return getInstalledAppsComprehensive()
    }
    
    private fun getInstalledAppsComprehensive(): List<AppInfo> {
        val packageManager = context.packageManager
        
        android.util.Log.d("AppRepository", "=== COMPREHENSIVE APP DETECTION ===")
        
        // Method 1: Get installed applications
        val installedApps = try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to get installed applications: ${e.message}")
            emptyList()
        }
        
        // Method 2: Get installed packages
        val installedPackages = try {
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to get installed packages: ${e.message}")
            emptyList()
        }
        
        android.util.Log.d("AppRepository", "Applications found: ${installedApps.size}")
        android.util.Log.d("AppRepository", "Packages found: ${installedPackages.size}")
        
        // Combine both approaches to get comprehensive list
        val allPackageNames = mutableSetOf<String>()
        allPackageNames.addAll(installedApps.map { it.packageName })
        allPackageNames.addAll(installedPackages.map { it.packageName })
        
        android.util.Log.d("AppRepository", "Total unique packages: ${allPackageNames.size}")
        
        val appsWithMicPermission = mutableListOf<AppInfo>()
        var processedCount = 0
        var micPermissionCount = 0
        
        allPackageNames.forEach { packageName ->
            processedCount++
            
            try {
                // Skip our own app and core system components
                if (packageName == context.packageName || 
                    packageName == "android" || 
                    packageName == "com.android.systemui") {
                    return@forEach
                }
                
                // Check if this package has microphone permission
                val hasMicPermission = hasMicrophonePermission(packageName)
                
                if (hasMicPermission) {
                    micPermissionCount++
                    
                    // Try to get application info
                    val appInfo = try {
                        packageManager.getApplicationInfo(packageName, 0)
                    } catch (e: Exception) {
                        android.util.Log.w("AppRepository", "Could not get app info for $packageName: ${e.message}")
                        null
                    }
                    
                    val appName = try {
                        appInfo?.loadLabel(packageManager)?.toString() ?: packageName
                    } catch (e: Exception) {
                        packageName
                    }
                    
                    val icon = try {
                        appInfo?.loadIcon(packageManager)
                    } catch (e: Exception) {
                        null
                    }
                    
                    val isSystemApp = appInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false
                    
                    val appInfoObj = AppInfo(
                        packageName = packageName,
                        appName = appName,
                        icon = icon,
                        isSystemApp = isSystemApp,
                        hasMicrophonePermission = true
                    )
                    
                    appsWithMicPermission.add(appInfoObj)
                    android.util.Log.d("AppRepository", "✓ Added: $appName ($packageName) - System: $isSystemApp")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("AppRepository", "Error processing package $packageName: ${e.message}")
            }
        }
        
        android.util.Log.d("AppRepository", "=== COMPREHENSIVE RESULTS ===")
        android.util.Log.d("AppRepository", "Processed packages: $processedCount")
        android.util.Log.d("AppRepository", "Apps with microphone permission: $micPermissionCount")
        android.util.Log.d("AppRepository", "Final list size: ${appsWithMicPermission.size}")
        
        return appsWithMicPermission.sortedBy { it.appName.lowercase() }
    }
    
    // Keep the old method for comparison
    suspend fun getInstalledAppsOldMethod(): List<AppInfo> {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        android.util.Log.d("AppRepository", "=== AppRepository.getInstalledApps() ===")
        android.util.Log.d("AppRepository", "Total installed apps: ${installedApps.size}")
        
        // Also try alternative method using getInstalledPackages
        val installedPackages = try {
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to get installed packages: ${e.message}")
            emptyList()
        }
        
        android.util.Log.d("AppRepository", "Total installed packages: ${installedPackages.size}")
        
        // Compare the two approaches
        val appPackages = installedApps.map { it.packageName }.toSet()
        val packageNames = installedPackages.map { it.packageName }.toSet()
        
        val onlyInApps = appPackages - packageNames
        val onlyInPackages = packageNames - appPackages
        
        android.util.Log.d("AppRepository", "Only in applications: ${onlyInApps.size}")
        android.util.Log.d("AppRepository", "Only in packages: ${onlyInPackages.size}")
        
        if (onlyInPackages.isNotEmpty()) {
            android.util.Log.d("AppRepository", "Additional packages found: ${onlyInPackages.joinToString(", ")}")
        }
        
        var beforeFilterCount = 0
        var afterFilterCount = 0
        var withMicPermissionCount = 0
        
        val filteredApps = installedApps
            .filter { app ->
                beforeFilterCount++
                
                // Much less restrictive filtering - only filter out core system processes
                val shouldInclude = app.packageName != context.packageName && // Not our own app
                        app.packageName != "android" && // Not the core Android system
                        app.packageName != "com.android.systemui" && // Not system UI
                        // Allow most other apps including system apps that might use microphone
                        try {
                            app.loadLabel(packageManager).toString().isNotBlank()
                        } catch (e: Exception) {
                            // If we can't load the label, still include it
                            true
                        }
                
                if (shouldInclude) {
                    afterFilterCount++
                }
                
                shouldInclude
            }
        
        android.util.Log.d("AppRepository", "After basic filtering: $afterFilterCount apps (from $beforeFilterCount)")
        
        val appsWithInfo = filteredApps
            .map { app ->
                val appName = try {
                    app.loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    app.packageName
                }
                
                val hasMicPermission = hasMicrophonePermission(app.packageName)
                
                if (hasMicPermission) {
                    withMicPermissionCount++
                    android.util.Log.d("AppRepository", "✓ App with mic permission: $appName (${app.packageName})")
                }
                
                AppInfo(
                    packageName = app.packageName,
                    appName = appName,
                    icon = try {
                        app.loadIcon(packageManager)
                    } catch (e: Exception) {
                        null
                    },
                    isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    hasMicrophonePermission = hasMicPermission
                )
            }
            .filter { it.hasMicrophonePermission } // Only include apps with microphone permission
            .sortedBy { it.appName.lowercase() }
        
        android.util.Log.d("AppRepository", "=== FINAL RESULTS ===")
        android.util.Log.d("AppRepository", "Apps with microphone permission: $withMicPermissionCount")
        android.util.Log.d("AppRepository", "Final app list size: ${appsWithInfo.size}")
        
        return appsWithInfo
    }
    
    private fun hasMicrophonePermission(packageName: String): Boolean {
        return hasMicrophonePermissionComprehensive(packageName)
    }
    
    private fun hasMicrophonePermissionComprehensive(packageName: String): Boolean {
        val packageManager = context.packageManager
        
        // Define all microphone-related permissions
        val microphonePermissions = setOf(
            android.Manifest.permission.RECORD_AUDIO,
            "android.permission.CAPTURE_AUDIO_OUTPUT",
            "android.permission.CAPTURE_AUDIO_HOTWORD",
            "android.permission.CAPTURE_SECURE_AUDIO_OUTPUT",
            "android.permission.RECORD_AUDIO_OUTPUT",
            "android.permission.RECORD"
        )
        
        // Method 1: Try to get package info with permissions
        try {
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS
            )
            
            val declaredPermissions = packageInfo.requestedPermissions
            if (declaredPermissions != null) {
                val hasPermission = declaredPermissions.any { permission ->
                    microphonePermissions.contains(permission)
                }
                
                if (hasPermission) {
                    android.util.Log.d("AppRepository", "✓ Method 1 - $packageName has microphone permission: ${declaredPermissions.filter { microphonePermissions.contains(it) }}")
                    return true
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "Method 1 failed for $packageName: ${e.message}")
        }
        
        // Method 2: Check if permission is granted (for runtime permissions)
        try {
            val permissionResult = packageManager.checkPermission(
                android.Manifest.permission.RECORD_AUDIO,
                packageName
            )
            
            if (permissionResult == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("AppRepository", "✓ Method 2 - $packageName has RECORD_AUDIO granted")
                return true
            }
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "Method 2 failed for $packageName: ${e.message}")
        }
        
        // Method 3: Try to get application info and check permissions differently
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            
            // Check if it's a system app that might have microphone access
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            
            if (isSystemApp) {
                // Some system apps might have microphone access through other means
                // Check if it's a known system app that uses microphone
                val knownMicrophoneSystemApps = setOf(
                    "com.android.soundrecorder",
                    "com.android.dialer",
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.apps.googleassistant",
                    "com.samsung.android.bixby.agent",
                    "com.samsung.android.visionintelligence",
                    "com.samsung.android.app.spage"
                )
                
                if (knownMicrophoneSystemApps.contains(packageName)) {
                    android.util.Log.d("AppRepository", "✓ Method 3 - $packageName is known system app with microphone")
                    return true
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "Method 3 failed for $packageName: ${e.message}")
        }
        
        // Method 4: Check if it's a communication app (likely to have microphone)
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = appInfo.loadLabel(packageManager).toString().lowercase()
            
            val microphoneKeywords = setOf(
                "call", "phone", "dialer", "voice", "recording", "recorder", 
                "music", "audio", "sound", "radio", "podcast", "chat", "message", 
                "whatsapp", "telegram", "discord", "zoom", "meet", "skype",
                "assistant", "siri", "bixby", "alexa", "google"
            )
            
            val hasKeyword = microphoneKeywords.any { keyword ->
                appName.contains(keyword) || packageName.lowercase().contains(keyword)
            }
            
            if (hasKeyword) {
                android.util.Log.d("AppRepository", "✓ Method 4 - $packageName ($appName) likely uses microphone based on name")
                return true
            }
            
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "Method 4 failed for $packageName: ${e.message}")
        }
        
        return false
    }
    
    // Keep the old method for comparison
    private fun hasMicrophonePermissionOldMethod(packageName: String): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS
            )
            
            // Check for all microphone-related permissions
            val microphonePermissions = setOf(
                android.Manifest.permission.RECORD_AUDIO,
                "android.permission.CAPTURE_AUDIO_OUTPUT", // Some apps use this
                "android.permission.CAPTURE_AUDIO_HOTWORD", // Voice assistants
                "android.permission.CAPTURE_SECURE_AUDIO_OUTPUT" // Secure audio
            )
            
            packageInfo.requestedPermissions?.any { permission ->
                microphonePermissions.contains(permission)
            } ?: false
        } catch (e: Exception) {
            // If we can't get package info, try alternative method
            android.util.Log.w("AppRepository", "Failed to check permissions for $packageName: ${e.message}")
            
            // Alternative check: try to see if the app has been granted RECORD_AUDIO
            try {
                val permissionResult = context.packageManager.checkPermission(
                    android.Manifest.permission.RECORD_AUDIO,
                    packageName
                )
                permissionResult == PackageManager.PERMISSION_GRANTED
            } catch (e2: Exception) {
                android.util.Log.w("AppRepository", "Alternative permission check failed for $packageName: ${e2.message}")
            false
            }
        }
    }
    
    val monitoredApps: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[monitoredAppsKey] ?: emptySet()
    }
    
    val isMonitoringEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[isMonitoringEnabledKey] ?: false
    }
    
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[notificationsEnabledKey] ?: true
    }
    

    
    val autoStart: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[autoStartKey] ?: false
    }
    
    val monitoringMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[monitoringModeKey] ?: "NON_COMMUNICATION_APPS"
    }
    
    val isDarkTheme: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[isDarkThemeKey] ?: true // Default to dark theme
    }
    
    suspend fun setMonitoredApps(apps: Set<String>) {
        dataStore.edit { preferences ->
            preferences[monitoredAppsKey] = apps
        }
    }
    
    suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[isMonitoringEnabledKey] = enabled
        }
    }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[notificationsEnabledKey] = enabled
        }
    }
    

    
    suspend fun setAutoStart(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[autoStartKey] = enabled
        }
    }
    
    suspend fun setMonitoringMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[monitoringModeKey] = mode
        }
    }
    
    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[isDarkThemeKey] = isDark
        }
    }
    
    suspend fun autoAddAppsWithMicrophonePermission() {
        val excludedPackages = setOf(
            "com.android.dialer",
            "com.samsung.android.dialer",
            "com.google.android.dialer",
            "com.whatsapp",
            "com.facebook.orca",
            "com.viber.voip",
            "org.telegram.messenger",
            "org.thoughtcrime.securesms",
            "com.google.android.apps.meetings",
            "us.zoom.videomeetings",
            "com.skype.raider",
            "com.instagram.android",
            "com.snapchat.android",
            "com.tencent.mm",
            "jp.naver.line.android",
            "com.kakao.talk"
        )
        val appsWithMicPermission = getInstalledApps()
            .filter { it.hasMicrophonePermission && it.packageName !in excludedPackages }
            .map { it.packageName }
            .toSet()
        
        // Log for debugging
        android.util.Log.d("AppRepository", "Found ${appsWithMicPermission.size} apps with microphone permission")
        appsWithMicPermission.forEach { packageName ->
            android.util.Log.d("AppRepository", "App with mic permission: $packageName")
        }
        
        // Replace the entire monitored apps list instead of adding to it
        setMonitoredApps(appsWithMicPermission)
    }
} 