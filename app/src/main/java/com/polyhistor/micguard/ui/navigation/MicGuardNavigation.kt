package com.polyhistor.micguard.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.polyhistor.micguard.SupportActivity
import com.polyhistor.micguard.ui.screens.AppListScreen
import com.polyhistor.micguard.ui.screens.MainScreen
import com.polyhistor.micguard.ui.screens.SettingsScreen
import com.polyhistor.micguard.viewmodel.MicGuardViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object AppList : Screen("app_list")
    object Settings : Screen("settings")
}

@Composable
fun MicGuardNavigation(
    viewModel: MicGuardViewModel,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAppList = {
                    navController.navigate(Screen.AppList.route)
                },
                onNavigateToSupport = {
                    val intent = Intent(context, SupportActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
        
        composable(Screen.AppList.route) {
            AppListScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
} 