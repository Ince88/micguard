package com.polyhistor.micguard.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.polyhistor.micguard.data.AppInfo
import com.polyhistor.micguard.data.MicrophoneEvent
import com.polyhistor.micguard.data.PreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class MicGuardAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "MicGuardAccessibility"
        private val _microphoneEvents = MutableStateFlow<List<MicrophoneEvent>>(emptyList())
        val microphoneEvents: StateFlow<List<MicrophoneEvent>> = _microphoneEvents
        
        private val activeRecordings = ConcurrentHashMap<String, Long>()
        private var monitoringJob: Job? = null
        private var lastRecordingCount = 0
        private var hasNotifiedCurrentSession = false
        private var micUsageStartTime: Long? = null
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val isMonitoringEnabledKey = booleanPreferencesKey("is_monitoring_enabled")
    private lateinit var dataStore: DataStore<Preferences>
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "MicGuard Accessibility Service Connected")
        
        // Initialize the singleton DataStore
        dataStore = PreferencesDataStore.getInstance(this)
        
        startConditionalMonitoring()
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We'll use audio recording configuration instead of accessibility events
        // This method is required but not used for microphone monitoring
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "MicGuard Accessibility Service Interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopMicrophoneMonitoring()
        Log.d(TAG, "MicGuard Accessibility Service Destroyed")
    }
    
    private fun startConditionalMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (true) {
                try {
                    // Check if monitoring is enabled in app settings
                    val isMonitoringEnabled = dataStore.data.first()[isMonitoringEnabledKey] ?: false
                    
                    if (isMonitoringEnabled) {
                        Log.d(TAG, "Monitoring is enabled - checking microphone usage")
                checkMicrophoneUsage()
                    } else {
                        Log.d(TAG, "Monitoring is disabled - skipping microphone check")
                        // Reset state when monitoring is disabled
                        if (lastRecordingCount > 0) {
                            lastRecordingCount = 0
                            hasNotifiedCurrentSession = false
                            micUsageStartTime = null
                            activeRecordings.clear()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in monitoring loop", e)
                }
                
                delay(1000) // Check every second
            }
        }
    }
    
    private fun stopMicrophoneMonitoring() {
        monitoringJob?.cancel()
        activeRecordings.clear()
        lastRecordingCount = 0
        hasNotifiedCurrentSession = false
        micUsageStartTime = null
    }
    
    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun checkMicrophoneUsage() {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val recordingConfigs = audioManager.getActiveRecordingConfigurations()
            
            val currentTime = System.currentTimeMillis()
            val currentRecordingCount = recordingConfigs.size
            
            // Log recording configuration details
            if (currentRecordingCount > 0) {
                Log.d(TAG, "Active recording configurations: $currentRecordingCount")
            }
            
            // If recording count changed, notify about microphone usage
            if (currentRecordingCount != lastRecordingCount) {
                Log.d(TAG, "Recording count changed: $lastRecordingCount -> $currentRecordingCount")
                
                if (currentRecordingCount > lastRecordingCount) {
                    // New recording started
                    Log.d(TAG, "Microphone usage detected")
                    val appInfo = getAppInfoFromRecordingConfig(recordingConfigs)
                    notifyMicrophoneUsageSuspend(appInfo)
                    hasNotifiedCurrentSession = true
                    micUsageStartTime = currentTime
                } else {
                    // Recording stopped
                    Log.d(TAG, "Microphone usage stopped")
                    hasNotifiedCurrentSession = false
                    micUsageStartTime?.let { startTime ->
                        val durationMillis = currentTime - startTime
                        NotificationService.showMicrophoneStoppedNotification(this@MicGuardAccessibilityService, durationMillis)
                    }
                    micUsageStartTime = null
                }
                lastRecordingCount = currentRecordingCount
            } else if (currentRecordingCount > 0 && !hasNotifiedCurrentSession) {
                // Recording is ongoing but we haven't notified yet (service started while recording was active)
                Log.d(TAG, "Microphone usage detected (ongoing session)")
                val appInfo = getAppInfoFromRecordingConfig(recordingConfigs)
                notifyMicrophoneUsageSuspend(appInfo)
                hasNotifiedCurrentSession = true
                micUsageStartTime = currentTime
            }
            
            // Debug logging
            Log.d(TAG, "Debug - currentRecordingCount: $currentRecordingCount, lastRecordingCount: $lastRecordingCount, hasNotifiedCurrentSession: $hasNotifiedCurrentSession")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking microphone usage", e)
        }
    }
    
    private fun getAppInfoFromRecordingConfig(recordingConfigs: List<AudioRecordingConfiguration>): AppInfo? {
        return try {
            Log.d(TAG, "Attempting to get app info from ${recordingConfigs.size} recording configs")
            
            if (recordingConfigs.isNotEmpty()) {
                val config = recordingConfigs.first()
                Log.d(TAG, "Recording config class: ${config.javaClass.name}")
                
                // Try to get client package name using reflection (API 29+)
                val clientPackageName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        Log.d(TAG, "Trying to get client package name using reflection")
                        val getClientPackageNameMethod = config.javaClass.getMethod("getClientPackageName")
                        val result = getClientPackageNameMethod.invoke(config) as? String
                        Log.d(TAG, "getClientPackageName result: $result")
                        result
                    } catch (e: Exception) {
                        Log.d(TAG, "Could not get client package name: ${e.message}")
                        Log.d(TAG, "Available methods: ${config.javaClass.methods.map { it.name }}")
                        null
                    }
                } else {
                    Log.d(TAG, "API level too low for getClientPackageName")
                    null
                }
                
                // If we couldn't get the package name directly, try using client UID
                if (clientPackageName == null) {
                    try {
                        Log.d(TAG, "Trying to get client UID")
                        val getClientUidMethod = config.javaClass.getMethod("getClientUid")
                        val clientUidAny = getClientUidMethod.invoke(config)
                        val clientUid = (clientUidAny as? Int)
                        Log.d(TAG, "Client UID: $clientUid")
                        if (clientUid == null) {
                            Log.d(TAG, "clientUid is null, cannot proceed")
                            return null
                        }
                        // Get package name from UID using package manager
                        val packageManager = packageManager
                        val packageNames = packageManager.getPackagesForUid(clientUid)
                        
                        if (packageNames != null && packageNames.isNotEmpty()) {
                            val packageName = packageNames[0] // Use the first package
                            Log.d(TAG, "Found package name from UID: $packageName")
                            
                            // Get app name from package manager
                            val appName = try {
                                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                                val label = packageManager.getApplicationLabel(applicationInfo).toString()
                                Log.d(TAG, "App name resolved from UID: $label")
                                label
                            } catch (e: Exception) {
                                Log.d(TAG, "Could not get app name for $packageName: ${e.message}")
                                packageName
                            }
                            
                            AppInfo(
                                packageName = packageName,
                                appName = appName,
                                icon = null,
                                isSystemApp = false,
                                hasMicrophonePermission = true
                            )
                        } else {
                            Log.d(TAG, "No packages found for UID: $clientUid")
                            null
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Could not get app info from UID: ${e.message}")
                        null
                    }
                } else {
                    Log.d(TAG, "Found app using microphone: $clientPackageName")
                    
                    // Get app name from package manager
                    val packageManager = packageManager
                    val appName = try {
                        val applicationInfo = packageManager.getApplicationInfo(clientPackageName, 0)
                        val label = packageManager.getApplicationLabel(applicationInfo).toString()
                        Log.d(TAG, "App name resolved: $label")
                        label
                    } catch (e: Exception) {
                        Log.d(TAG, "Could not get app name for $clientPackageName: ${e.message}")
                        clientPackageName
                    }
                    
                    AppInfo(
                        packageName = clientPackageName,
                        appName = appName,
                        icon = null,
                        isSystemApp = false,
                        hasMicrophonePermission = true
                    )
                }
            } else {
                Log.d(TAG, "No recording configs available")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app info from recording config", e)
            null
        }
    }
    
    private fun addMicrophoneEvent(event: MicrophoneEvent) {
        val currentEvents = _microphoneEvents.value.toMutableList()
        currentEvents.add(event)
        
        // Keep only last 100 events
        if (currentEvents.size > 100) {
            currentEvents.removeAt(0)
        }
        
        _microphoneEvents.value = currentEvents
    }

    private suspend fun notifyMicrophoneUsageSuspend(appInfo: AppInfo? = null) {
        Log.d(TAG, "notifyMicrophoneUsage called with appInfo: ${appInfo?.appName}")
        
        // Send broadcast to notify the main app
        val intent = Intent("com.polyhistor.micguard.MICROPHONE_USAGE")
        intent.putExtra("timestamp", System.currentTimeMillis())
        if (appInfo != null) {
            intent.putExtra("package_name", appInfo.packageName)
            intent.putExtra("app_name", appInfo.appName)
            Log.d(TAG, "Added app info to broadcast: ${appInfo.appName}")
        }
        intent.setPackage(packageName)
        sendBroadcast(intent)
        
        // Show notification with app info if available
        if (appInfo != null) {
            Log.d(TAG, "Showing notification with app name: ${appInfo.appName}")
            NotificationService.showMicrophoneNotification(this@MicGuardAccessibilityService, appInfo.appName)
        } else {
            Log.d(TAG, "Showing notification without app name")
            NotificationService.showMicrophoneNotification(this@MicGuardAccessibilityService)
        }
    }
} 