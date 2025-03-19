package jarvay.workpaper.compose

import android.annotation.SuppressLint
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jarvay.workpaper.compose.album.AlbumDetailScreen
import jarvay.workpaper.compose.album.DirsRelationScreen
import jarvay.workpaper.compose.components.LocalMainActivityModel
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.LocalSnackbarHostState
import jarvay.workpaper.compose.components.SimpleSnackbar
import jarvay.workpaper.compose.components.UpgradeDialog
import jarvay.workpaper.compose.home.HomeScreen
import jarvay.workpaper.compose.rule.RuleCreateScreen
import jarvay.workpaper.compose.rule.RuleUpdateScreen
import jarvay.workpaper.compose.settings.SettingsScreen
import jarvay.workpaper.compose.sponsor.SponsorScreen
import jarvay.workpaper.compose.style.StyleCreateScreen
import jarvay.workpaper.compose.style.StyleUpdateScreen
import jarvay.workpaper.others.Global
import jarvay.workpaper.viewModel.MainActivityViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun WorkpaperApp(mainActivityViewModel: MainActivityViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val hostState = remember {
        SnackbarHostState()
    }

    val simpleSnackbar = SimpleSnackbar(
        scope = scope,
        hostState = hostState,
        context = context
    )
    Global.workpaperAppScope = scope
    Global.snackbarHostState = hostState

    CompositionLocalProvider(
        LocalMainActivityModel provides mainActivityViewModel,
        LocalSnackbarHostState provides hostState,
        LocalSimpleSnackbar provides simpleSnackbar
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = hostState)
            }
        ) { _ ->
            WorkpaperNavHost(
                navController = navController,
            )

            UpgradeDialog(viewModal = mainActivityViewModel, simpleSnackbar = simpleSnackbar)
        }
    }
}

@Composable
fun WorkpaperNavHost(
    navController: NavHostController,
) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                navController = navController,
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
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
        composable(route = Screen.DirsRelation.route) {
            DirsRelationScreen(navController = navController)
        }

        composable(route = Screen.Sponsor.route) {
            SponsorScreen(navController = navController)
        }

        composable(route = Screen.StyleCreate.route) {
            StyleCreateScreen(navController = navController)
        }
        composable(route = Screen.StyleUpdate.route) {
            StyleUpdateScreen(navController = navController)
        }
    }
}
