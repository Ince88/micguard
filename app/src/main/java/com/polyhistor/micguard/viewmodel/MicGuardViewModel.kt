package com.polyhistor.micguard.viewmodel

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polyhistor.micguard.data.AppInfo
import com.polyhistor.micguard.data.AppRepository
import com.polyhistor.micguard.data.MicrophoneEvent
import com.polyhistor.micguard.service.MicGuardAccessibilityService
import com.polyhistor.micguard.service.MicGuardForegroundService
import com.polyhistor.micguard.service.NotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MicGuardViewModel(
    private val appRepository: AppRepository,
    private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MicGuardUiState())
    val uiState: StateFlow<MicGuardUiState> = _uiState.asStateFlow()
    
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    
    private val _microphoneEvents = MutableStateFlow<List<MicrophoneEvent>>(emptyList())
    val microphoneEvents: StateFlow<List<MicrophoneEvent>> = _microphoneEvents.asStateFlow()
    
    init {
        // Initial permission check
        updatePermissions()
        
        viewModelScope.launch {
            // Collect repository flows separately and update UI state
            appRepository.isMonitoringEnabled.collect { isMonitoring ->
                _uiState.value = _uiState.value.copy(
                    isMonitoringEnabled = isMonitoring
                )
            }
        }
        
        viewModelScope.launch {
            appRepository.monitoredApps.collect { monitoredApps ->
                _uiState.value = _uiState.value.copy(
                    monitoredApps = monitoredApps
                )
            }
        }
        
        viewModelScope.launch {
            appRepository.notificationsEnabled.collect { notificationsEnabled ->
                _uiState.value = _uiState.value.copy(
                    notificationsEnabled = notificationsEnabled
                )
            }
        }
        
        viewModelScope.launch {
            appRepository.autoStart.collect { autoStart ->
                _uiState.value = _uiState.value.copy(
                    autoStart = autoStart
                )
            }
        }
        
        viewModelScope.launch {
            appRepository.monitoringMode.collect { monitoringModeStr ->
                val monitoringMode = try {
                    MonitoringMode.valueOf(monitoringModeStr)
                } catch (e: Exception) {
                    MonitoringMode.NON_COMMUNICATION_APPS
                }
                _uiState.value = _uiState.value.copy(
                    monitoringMode = monitoringMode
                )
            }
        }
        
        viewModelScope.launch {
            appRepository.isDarkTheme.collect { isDarkTheme ->
                _uiState.value = _uiState.value.copy(
                    isDarkTheme = isDarkTheme
                )
            }
        }
        
        loadApps()
        setupNotificationChannels()
    }
    
    fun loadApps() {
        viewModelScope.launch {
            try {
                android.util.Log.d("MicGuardViewModel", "Loading apps...")
                val installedApps = appRepository.getInstalledApps()
                android.util.Log.d("MicGuardViewModel", "Loaded ${installedApps.size} apps with microphone permission")
                
                // Log some details about the loaded apps
                installedApps.forEach { app ->
                    android.util.Log.d("MicGuardViewModel", "App: ${app.appName} (${app.packageName}) - System: ${app.isSystemApp}")
                }
                
                _apps.value = installedApps
            } catch (e: Exception) {
                android.util.Log.e("MicGuardViewModel", "Error loading apps", e)
            }
        }
    }
    
    fun refreshApps() {
        android.util.Log.d("MicGuardViewModel", "Manually refreshing app list...")
        loadApps()
    }
    
    fun toggleMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            android.util.Log.d("MicGuardViewModel", "toggleMonitoring called with enabled: $enabled")
            
            // Update the monitoring state first
            appRepository.setMonitoringEnabled(enabled)
            
            if (enabled) {
                android.util.Log.d("MicGuardViewModel", "Starting monitoring services...")
                startMonitoring()
            } else {
                android.util.Log.d("MicGuardViewModel", "Stopping monitoring services...")
                stopMonitoring()
            }
            
            // Log the final state
            android.util.Log.d("MicGuardViewModel", "Monitoring toggle completed. New state: $enabled")
        }
    }
    
    fun toggleAppMonitoring(packageName: String, monitored: Boolean) {
        viewModelScope.launch {
            val currentMonitored = _uiState.value.monitoredApps.toMutableSet()
            if (monitored) {
                currentMonitored.add(packageName)
            } else {
                currentMonitored.remove(packageName)
            }
            appRepository.setMonitoredApps(currentMonitored)
        }
    }
    
    fun updateNotificationSettings(enabled: Boolean) {
        viewModelScope.launch {
            appRepository.setNotificationsEnabled(enabled)
        }
    }
    
    fun updateAutoStart(enabled: Boolean) {
        viewModelScope.launch {
            appRepository.setAutoStart(enabled)
        }
    }
    
    fun updateDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            appRepository.setDarkTheme(isDark)
        }
    }
    
    fun autoAddAppsWithMicrophonePermission() {
        viewModelScope.launch {
            android.util.Log.d("MicGuardViewModel", "Auto-adding apps with microphone permission...")
            appRepository.autoAddAppsWithMicrophonePermission()
            // Refresh the app list after auto-adding
            loadApps()
        }
    }
    
    fun initializeMonitoredApps() {
        viewModelScope.launch {
            // Auto-add apps with microphone permissions if no apps are currently monitored
            if (uiState.value.monitoredApps.isEmpty()) {
                appRepository.autoAddAppsWithMicrophonePermission()
            }
        }
    }
    
    fun setMonitoringMode(mode: MonitoringMode) {
        viewModelScope.launch {
            when (mode) {
                MonitoringMode.NON_COMMUNICATION_APPS -> {
                    appRepository.autoAddAppsWithMicrophonePermission()
                }
                MonitoringMode.INCLUDE_COMMUNICATION_APPS -> {
                    // Add all apps with microphone permissions including communication apps
                    val allAppsWithMic = appRepository.getInstalledApps()
                        .filter { it.hasMicrophonePermission }
                        .map { it.packageName }
                        .toSet()
                    appRepository.setMonitoredApps(allAppsWithMic)
                }
                MonitoringMode.CUSTOM -> {
                    // Keep current custom selection
                }
            }
            
            appRepository.setMonitoringMode(mode.name)
        }
    }
    
    private fun startMonitoring() {
        android.util.Log.d("MicGuardViewModel", "Starting foreground service...")
        MicGuardForegroundService.startService(context)
        NotificationService.createNotificationChannel(context)
        android.util.Log.d("MicGuardViewModel", "Foreground service started. Accessibility service should now detect the enabled state.")
    }
    
    private fun stopMonitoring() {
        android.util.Log.d("MicGuardViewModel", "Stopping foreground service...")
        MicGuardForegroundService.stopService(context)
        NotificationService.cancelNotifications(context)
        android.util.Log.d("MicGuardViewModel", "Foreground service stopped. Accessibility service should now detect the disabled state.")
    }
    
    private fun setupNotificationChannels() {
        NotificationService.createNotificationChannel(context)
    }
    
    fun isAccessibilityServiceEnabled(): Boolean {
        try {
            // Check if accessibility is globally enabled
            val accessibilityEnabled = try {
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
            } catch (e: Settings.SettingNotFoundException) {
                0
            }
            
            Log.d("MicGuardViewModel", "Global accessibility enabled: $accessibilityEnabled")
            
            // Check if our service is in the enabled services list
            val service = "${context.packageName}/com.polyhistor.micguard.service.MicGuardAccessibilityService"
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            Log.d("MicGuardViewModel", "Service: $service, SettingValue: $settingValue")
            
            val isEnabled = settingValue?.contains(service) == true
            Log.d("MicGuardViewModel", "Accessibility service enabled: $isEnabled")
            
            // If the service is not in the settings but accessibility is globally enabled,
            // be more lenient and allow it (some devices have this issue)
            if (!isEnabled && accessibilityEnabled == 1) {
                Log.d("MicGuardViewModel", "Service not in settings but accessibility is globally enabled - allowing")
                return true
            }
            
            // If the user has been shown accessibility settings before, be more lenient
            val sharedPrefs = context.getSharedPreferences("MicGuardPrefs", Context.MODE_PRIVATE)
            val settingsShown = sharedPrefs.getBoolean("accessibility_settings_shown", false)
            if (settingsShown && accessibilityEnabled == 1) {
                Log.d("MicGuardViewModel", "User has been shown accessibility settings before - allowing")
                return true
            }
            
            return isEnabled
        } catch (e: Exception) {
            Log.e("MicGuardViewModel", "Error checking accessibility service", e)
            return false
        }
    }
    
    fun isUsageStatsPermissionGranted(): Boolean {
        try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            val isGranted = mode == android.app.AppOpsManager.MODE_ALLOWED
            Log.d("MicGuardViewModel", "Usage stats permission granted: $isGranted, mode: $mode")
            return isGranted
        } catch (e: Exception) {
            Log.e("MicGuardViewModel", "Error checking usage stats permission", e)
            return false
        }
    }
    
    fun openAccessibilitySettings() {
        try {
            // Try to open directly to our app's accessibility service settings
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            
            // Store a flag that the user has been directed to accessibility settings
            val sharedPrefs = context.getSharedPreferences("MicGuardPrefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("accessibility_settings_shown", true).apply()
        } catch (e: Exception) {
            Log.e("MicGuardViewModel", "Error opening accessibility settings", e)
            // Fallback to general accessibility settings
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }
    
    fun openUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    fun openNotificationSettings() {
        val intent = Intent().apply {
            when {
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                else -> {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", android.os.Process.myUid())
                }
            }
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    private fun updatePermissions() {
        val accessibilityPermission = isAccessibilityServiceEnabled()
        val usageStatsPermission = isUsageStatsPermissionGranted()
        
        Log.d("MicGuardViewModel", "Updating permissions - Accessibility: $accessibilityPermission, UsageStats: $usageStatsPermission")
        
        _uiState.value = _uiState.value.copy(
            hasAccessibilityPermission = accessibilityPermission,
            hasUsageStatsPermission = usageStatsPermission
        )
    }
    
    fun refreshPermissions() {
        viewModelScope.launch {
            // Small delay to ensure system has updated permission state
            kotlinx.coroutines.delay(100)
            updatePermissions()
        }
    }
}

enum class MonitoringMode {
    NON_COMMUNICATION_APPS,
    INCLUDE_COMMUNICATION_APPS,
    CUSTOM
}

data class MicGuardUiState(
    val isMonitoringEnabled: Boolean = false,
    val monitoredApps: Set<String> = emptySet(),
    val monitoringMode: MonitoringMode = MonitoringMode.NON_COMMUNICATION_APPS,
    val notificationsEnabled: Boolean = true,
    val autoStart: Boolean = false,
    val hasAccessibilityPermission: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val isDarkTheme: Boolean = true // Default to dark theme
) {
    val isReady: Boolean
        get() = hasAccessibilityPermission && hasUsageStatsPermission
    
    val statusText: String
        get() = when {
            !hasAccessibilityPermission -> "Accessibility permission required"
            !hasUsageStatsPermission -> "Usage stats permission required"
            isMonitoringEnabled -> "Monitoring active"
            else -> "Monitoring disabled"
        }
    
    val statusColor: androidx.compose.ui.graphics.Color
        get() = when {
            !hasAccessibilityPermission || !hasUsageStatsPermission -> androidx.compose.ui.graphics.Color.Red
            isMonitoringEnabled -> androidx.compose.ui.graphics.Color.Green
            else -> androidx.compose.ui.graphics.Color.Gray
        }
} 