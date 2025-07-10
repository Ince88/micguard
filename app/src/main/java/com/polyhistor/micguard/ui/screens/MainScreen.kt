package com.polyhistor.micguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.polyhistor.micguard.R
import com.polyhistor.micguard.data.AppInfo

import com.polyhistor.micguard.viewmodel.MicGuardUiState
import com.polyhistor.micguard.viewmodel.MicGuardViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import android.Manifest

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MicGuardViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAppList: () -> Unit,
    onNavigateToSupport: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val apps by viewModel.apps.collectAsState()
    
    // Permissions to request
    val permissions = listOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.FOREGROUND_SERVICE_MICROPHONE
    )
    val permissionsState = rememberMultiplePermissionsState(permissions)
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showComprehensivePermissionDialog by remember { mutableStateOf(false) }
    
    // Refresh permissions when the screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }
    
    // Listen for permission changes and refresh UI
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.refreshPermissions()
        }
    }
    
    // Request permissions on first launch if not granted
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }
    
    // If basic permissions are not granted, show dialog
    if (!permissionsState.allPermissionsGranted) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text("Permissions Required") },
            text = {
                Text("MicGuard needs microphone and foreground service permissions to monitor microphone usage. Please grant both permissions.")
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    permissionsState.launchMultiplePermissionRequest()
                }) {
                    Text("Grant Permissions")
                }
            }
        )
    } else {
        // Only show the main UI if basic permissions are granted
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 48.dp, // Add extra top padding to avoid notification bar
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
            ) {
                item {
                    HeaderSection(uiState = uiState)
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                item {
                    StatusCard(
                        uiState = uiState, 
                        onToggleMonitoring = { enabled ->
                            handleMonitoringToggle(
                                enabled = enabled,
                                uiState = uiState,
                                viewModel = viewModel,
                                onShowPermissionDialog = { showComprehensivePermissionDialog = true }
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    QuickActionsCard(
                        onNavigateToAppList = onNavigateToAppList,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToSupport = onNavigateToSupport
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    AppsWithMicrophoneCard(
                        apps = apps,
                        onNavigateToAppList = onNavigateToAppList
                    )
                }
            }
            }
            
        // Comprehensive Permission Dialog
        if (showComprehensivePermissionDialog) {
            ComprehensivePermissionDialog(
                uiState = uiState,
                onDismiss = { 
                    showComprehensivePermissionDialog = false
                    // Refresh permissions when dialog is dismissed in case user granted them
                    viewModel.refreshPermissions()
                },
                onGrantAccessibility = {
                    viewModel.openAccessibilitySettings()
                    showComprehensivePermissionDialog = false
                },
                onGrantUsageStats = {
                    viewModel.openUsageStatsSettings()
                    showComprehensivePermissionDialog = false
                },
                onEnableMonitoring = {
                    viewModel.toggleMonitoring(true)
                    showComprehensivePermissionDialog = false
                }
            )
        }
    }
}

private fun handleMonitoringToggle(
    enabled: Boolean,
    uiState: MicGuardUiState,
    viewModel: MicGuardViewModel,
    onShowPermissionDialog: () -> Unit
) {
    if (enabled) {
        // User wants to enable monitoring
        when {
            !uiState.hasAccessibilityPermission -> {
                // Need accessibility permission
                onShowPermissionDialog()
            }
            !uiState.hasUsageStatsPermission -> {
                // Need usage stats permission
                onShowPermissionDialog()
            }
            else -> {
                // All permissions granted, enable monitoring
                viewModel.toggleMonitoring(true)
            }
        }
                        } else {
        // User wants to disable monitoring - always allow
        viewModel.toggleMonitoring(false)
    }
}

@Composable
private fun ComprehensivePermissionDialog(
    uiState: MicGuardUiState,
    onDismiss: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onGrantUsageStats: () -> Unit,
    onEnableMonitoring: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Complete Setup to Enable Monitoring",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "MicGuard requires additional permissions to monitor microphone usage effectively:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Accessibility Permission Status
                PermissionStatusRow(
                    title = "Accessibility Service",
                    description = "Required to detect microphone usage",
                    isGranted = uiState.hasAccessibilityPermission,
                    onClick = if (!uiState.hasAccessibilityPermission) onGrantAccessibility else null
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Usage Stats Permission Status
                PermissionStatusRow(
                    title = "Usage Stats",
                    description = "Required to identify which apps are active",
                    isGranted = uiState.hasUsageStatsPermission,
                    onClick = if (!uiState.hasUsageStatsPermission) onGrantUsageStats else null
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (uiState.hasAccessibilityPermission && uiState.hasUsageStatsPermission) {
                    Text(
                        text = "✅ All permissions granted! You can now enable monitoring.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Please grant the required permissions above to enable monitoring.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (uiState.hasAccessibilityPermission && uiState.hasUsageStatsPermission) {
                androidx.compose.material3.Button(
                    onClick = onEnableMonitoring
                ) {
                    Text("Enable Monitoring")
                }
            } else {
                androidx.compose.material3.TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PermissionStatusRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Text(
                    text = "✓",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "!",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Action Button
        if (!isGranted && onClick != null) {
            androidx.compose.material3.OutlinedButton(
                onClick = onClick,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Grant", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HeaderSection(uiState: MicGuardUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.main_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = stringResource(R.string.main_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        StatusIndicator(status = uiState.statusText, color = uiState.statusColor)
    }
}

@Composable
private fun StatusIndicator(status: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusCard(
    uiState: MicGuardUiState,
    onToggleMonitoring: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.monitoring_status),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = uiState.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show additional info if permissions are missing
                    if (!uiState.isReady && !uiState.isMonitoringEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to set up permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Switch(
                    checked = uiState.isMonitoringEnabled,
                    onCheckedChange = onToggleMonitoring,
                    enabled = true // Always enable the switch
                )
            }
            
            // Show permission status indicators
            if (!uiState.isReady) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column {
                    Text(
                        text = "Required Setup:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Accessibility Permission
                    PermissionStatusRowCompact(
                        title = "Accessibility Service",
                        isGranted = uiState.hasAccessibilityPermission
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Usage Stats Permission
                    PermissionStatusRowCompact(
                        title = "Usage Statistics",
                        isGranted = uiState.hasUsageStatsPermission
                )
            }
        }
        }
    }
}

@Composable
private fun PermissionStatusRowCompact(
    title: String,
    isGranted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Status Icon
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Text(
                    text = "✓",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "!",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isGranted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsCard(
    onNavigateToAppList: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSupport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                ActionButton(
                    icon = Icons.Default.Notifications,
                    text = "Apps",
                    onClick = onNavigateToAppList
                )
                
                ActionButton(
                    icon = Icons.Default.Settings,
                    text = "Settings",
                    onClick = onNavigateToSettings
                )
                
                ActionButton(
                    icon = null,
                    text = "Tip Jar",
                    emoji = "☕",
                    onClick = onNavigateToSupport
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector? = null,
    text: String,
    emoji: String? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        ),
                        radius = 56f
                    ),
                    shape = CircleShape
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
            } else if (emoji != null) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsWithMicrophoneCard(
    apps: List<AppInfo>,
    onNavigateToAppList: () -> Unit
) {
    val appsWithMic = apps.filter { it.hasMicrophonePermission }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Apps with Microphone Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "${appsWithMic.size} apps found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = onNavigateToAppList
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "View Apps"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Show first 3 apps as examples
            appsWithMic.take(3).forEach { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = app.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            if (appsWithMic.size > 3) {
                Text(
                    text = "+${appsWithMic.size - 3} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 