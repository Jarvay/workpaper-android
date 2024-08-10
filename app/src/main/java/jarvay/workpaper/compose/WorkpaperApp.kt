package jarvay.workpaper.compose

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jarvay.workpaper.compose.album.AlbumDetailScreen
import jarvay.workpaper.compose.home.HomeScreen
import jarvay.workpaper.compose.rule.RuleCreateScreen
import jarvay.workpaper.compose.rule.RuleListScreen
import jarvay.workpaper.compose.rule.RuleUpdateScreen
import jarvay.workpaper.compose.settings.SettingsScreen

@Composable
fun WorkpaperApp() {
    val navController = rememberNavController()
    WorkpaperNavHost(
        navController = navController
    )
}

@Composable
fun WorkpaperNavHost(
    navController: NavHostController
) {
    val activity = (LocalContext.current as Activity)
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                navController = navController
            )
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(route = Screen.DayDetail.route) {
            RuleListScreen(navController = navController)
        }
        composable(route = Screen.RuleCreate.route) {
            RuleCreateScreen(navController = navController)
        }
        composable(route = Screen.RuleUpdate.route) {
            RuleUpdateScreen(navController = navController)
        }

        composable(route = Screen.AlbumDetail.route) {
            AlbumDetailScreen(navController = navController)
        }
    }
}
