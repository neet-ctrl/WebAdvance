package com.cylonid.nativealpha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cylonid.nativealpha.ui.theme.ThemeState
import com.cylonid.nativealpha.ui.theme.WAOSTheme
import com.cylonid.nativealpha.ui.screens.MainDashboardScreen
import com.cylonid.nativealpha.ui.screens.AddWebAppScreen
import com.cylonid.nativealpha.ui.screens.SettingsScreen
import com.cylonid.nativealpha.ui.screens.PermissionsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.cylonid.nativealpha.ui.WebViewFloatingOverlay.activeOverlay?.dismiss()
        setContent {
            WAOSTheme(themeMode = ThemeState.mode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WAOSNavGraph()
                }
            }
        }
    }
}

@Composable
fun WAOSNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            MainDashboardScreen(navController = navController)
        }
        composable("add_webapp") {
            AddWebAppScreen(
                navController = navController,
                onWebAppAdded = { navController.popBackStack() }
            )
        }
        composable(
            "edit_webapp/{webAppId}",
            arguments = listOf(navArgument("webAppId") { type = NavType.LongType })
        ) { backStackEntry ->
            val webAppId = backStackEntry.arguments?.getLong("webAppId") ?: 0L
            AddWebAppScreen(
                navController = navController,
                editWebAppId = webAppId,
                onWebAppAdded = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("settings/permissions") {
            PermissionsScreen(onBackPressed = { navController.popBackStack() })
        }
    }
}
