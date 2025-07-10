package com.polyhistor.micguard

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.polyhistor.micguard.data.AppRepository
import com.polyhistor.micguard.receiver.MicrophoneUsageReceiver
import com.polyhistor.micguard.service.NotificationService
import com.polyhistor.micguard.ui.navigation.MicGuardNavigation
import com.polyhistor.micguard.ui.theme.MicGuardTheme
import com.polyhistor.micguard.viewmodel.MicGuardViewModel
import com.polyhistor.micguard.viewmodel.MicGuardViewModelFactory

class MainActivity : ComponentActivity() {
    
    private lateinit var appRepository: AppRepository
    private lateinit var viewModel: MicGuardViewModel
    private var microphoneUsageReceiver: MicrophoneUsageReceiver? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize repository and ViewModel
        appRepository = AppRepository(this)
        viewModel = ViewModelProvider(
            this,
            MicGuardViewModelFactory(appRepository, this)
        )[MicGuardViewModel::class.java]
        
        // Setup notification channels
        NotificationService.createNotificationChannel(this)
        
        // Register broadcast receiver
        microphoneUsageReceiver = MicrophoneUsageReceiver.register(this)
        
        // Debug: Print all apps with microphone permissions
        debugPrintAppsWithMicrophonePermission()
        
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            
            MicGuardTheme(
                darkTheme = uiState.isDarkTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MicGuardNavigation(viewModel = viewModel)
                }
            }
        }
        
        // Handle intent extras (e.g., from notification)
        handleIntentExtras()
                }
    
    override fun onResume() {
        super.onResume()
        
        // Refresh permissions when returning from settings
        // This ensures the UI updates if user granted permissions while in settings
        if (::viewModel.isInitialized) {
        viewModel.refreshPermissions()
        }
        
        // Handle intent extras (e.g., from notification)
        handleIntentExtras()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister broadcast receiver
        microphoneUsageReceiver?.let { receiver ->
            unregisterReceiver(receiver)
}
    }
    
    private fun handleIntentExtras() {
        intent?.let { intent ->
            val packageName = intent.getStringExtra("package_name")
            if (packageName != null) {
                // Handle notification tap - could navigate to specific app details
                // For now, just refresh the UI
                viewModel.loadApps()
            }
        }
    }
    
    private fun debugPrintAppsWithMicrophonePermission() {
        try {
            val packageManager = packageManager
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            
            Log.d("MainActivity", "=== COMPREHENSIVE APP DETECTION DEBUG ===")
            Log.d("MainActivity", "Total installed apps: ${installedApps.size}")
            
            var totalWithMicPermission = 0
            var filteredOut = 0
            var permissionCheckFailed = 0
            var noLabelApps = 0
            
            val allAppsWithMic = mutableListOf<String>()
            val filteredApps = mutableListOf<String>()
            val failedApps = mutableListOf<String>()
            
            installedApps.forEach { app ->
                try {
                    // Check if app has microphone permission
                    var hasMicPermission = false
                    var permissionCheckSuccess = false
                    
                    try {
                        val packageInfo = packageManager.getPackageInfo(
                            app.packageName,
                            PackageManager.GET_PERMISSIONS
                        )
                        
                        val microphonePermissions = setOf(
                            android.Manifest.permission.RECORD_AUDIO,
                            "android.permission.CAPTURE_AUDIO_OUTPUT",
                            "android.permission.CAPTURE_AUDIO_HOTWORD",
                            "android.permission.CAPTURE_SECURE_AUDIO_OUTPUT"
                        )
                        
                        hasMicPermission = packageInfo.requestedPermissions?.any { permission ->
                            microphonePermissions.contains(permission)
                        } ?: false
                        
                        permissionCheckSuccess = true
                        
                        if (hasMicPermission) {
                            Log.d("MainActivity", "✓ RECORD_AUDIO: ${app.packageName} - ${packageInfo.requestedPermissions?.joinToString(", ") { it }}")
                        }
                        
                    } catch (e: Exception) {
                        // Try alternative method
                        try {
                            val permissionResult = packageManager.checkPermission(
                                android.Manifest.permission.RECORD_AUDIO,
                                app.packageName
                            )
                            hasMicPermission = permissionResult == PackageManager.PERMISSION_GRANTED
                            permissionCheckSuccess = true
                            
                            if (hasMicPermission) {
                                Log.d("MainActivity", "✓ GRANTED: ${app.packageName} (fallback method)")
                            }
                        } catch (e2: Exception) {
                            permissionCheckFailed++
                            failedApps.add(app.packageName)
                            Log.w("MainActivity", "✗ FAILED: ${app.packageName} - ${e2.message}")
                        }
                    }
                    
                    if (hasMicPermission && permissionCheckSuccess) {
                        totalWithMicPermission++
                        allAppsWithMic.add(app.packageName)
                        
                        val appName = try {
                            app.loadLabel(packageManager).toString()
                        } catch (e: Exception) {
                            noLabelApps++
                            app.packageName
                        }
                        
                        val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        
                        // Check if this app would be filtered out by our current logic
                        val wouldBeFiltered = app.packageName == packageName ||
                                app.packageName.startsWith("com.android.systemui") ||
                                app.packageName.startsWith("com.android.settings") ||
                                app.packageName == "com.google.android.gms" ||
                                app.packageName == "android" ||
                                appName.isBlank()
                        
                        if (wouldBeFiltered) {
                            filteredOut++
                            filteredApps.add(app.packageName)
                            Log.d("MainActivity", "⚠ FILTERED: $appName (${app.packageName}) - System: $isSystem")
                        } else {
                            Log.d("MainActivity", "✓ INCLUDED: $appName (${app.packageName}) - System: $isSystem")
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error processing app ${app.packageName}: ${e.message}")
                }
            }
            
            Log.d("MainActivity", "=== DETECTION SUMMARY ===")
            Log.d("MainActivity", "Total apps with microphone permission: $totalWithMicPermission")
            Log.d("MainActivity", "Apps filtered out by current logic: $filteredOut")
            Log.d("MainActivity", "Apps with permission check failures: $permissionCheckFailed")
            Log.d("MainActivity", "Apps with no label: $noLabelApps")
            Log.d("MainActivity", "Final apps that should show: ${totalWithMicPermission - filteredOut}")
            
            if (filteredApps.isNotEmpty()) {
                Log.d("MainActivity", "=== FILTERED APPS ===")
                filteredApps.forEach { pkg ->
                    Log.d("MainActivity", "Filtered: $pkg")
                }
            }
            
            if (failedApps.isNotEmpty()) {
                Log.d("MainActivity", "=== FAILED APPS ===")
                failedApps.forEach { pkg ->
                    Log.d("MainActivity", "Failed: $pkg")
                }
            }
            
            // Also check what apps are actually being returned by our AppRepository
            Log.d("MainActivity", "=== TESTING APPREPOSITORY ===")
            // This would be async, but let's at least log the attempt
            Log.d("MainActivity", "AppRepository detection will be logged separately by the repository")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in comprehensive debug function", e)
        }
    }
}