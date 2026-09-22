package jarvay.workpaper.compose

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
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
import jarvay.workpaper.compose.settings.LiveWallpaperSettingsScreen
import jarvay.workpaper.compose.settings.SettingsScreen
import jarvay.workpaper.compose.sponsor.SponsorScreen
import jarvay.workpaper.compose.style.StyleCreateScreen
import jarvay.workpaper.compose.style.StyleUpdateScreen
import jarvay.workpaper.others.Global
import jarvay.workpaper.viewModel.MainActivityViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.rememberNavController

@Composable
fun WorkpaperApp(mainActivityViewModel: MainActivityViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    val navigationEventDispatcherOwner = rememberNavigationEventDispatcherOwner(
        enabled = false,
        parent = null
    )

    val navController = rememberNavController<Route>(Route.Home)
    val activityFactoryOwner =
        LocalViewModelStoreOwner.current as HasDefaultViewModelProviderFactory

    val onNavigate: (Route) -> Unit = { route ->
        if (route is Route.Home) {
            navController.pop()
        } else {
            navController.push(route)
        }
    }

    BackHandler(enabled = navController.backStack.size > 1) {
        navController.pop()
    }

    CompositionLocalProvider(
        LocalMainActivityModel provides mainActivityViewModel,
        LocalSnackbarHostState provides hostState,
        LocalSimpleSnackbar provides simpleSnackbar,
        LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(state = hostState)
            }
        ) { _ ->
            NavDisplay(navController = navController) {
                entry<Route.Home> {
                    HiltNavEntry(factoryOwner = activityFactoryOwner) {
                        HomeScreen(onNavigate = onNavigate)
                    }
                }

                entry<Route.Settings> {
                    HiltNavEntry(factoryOwner = activityFactoryOwner) {
                        SettingsScreen(onNavigate = onNavigate)
                    }
                }

                entry<Route.LiveWallpaperSettings> {
                    HiltNavEntry(factoryOwner = activityFactoryOwner) {
                        LiveWallpaperSettingsScreen(onNavigate = onNavigate)
                    }
                }

                entry<Route.RuleCreate> {
                    HiltNavEntry(factoryOwner = activityFactoryOwner) {
                        RuleCreateScreen(onNavigate = onNavigate)
                    }
                }

                entry<Route.RuleUpdate> { route ->
                    HiltNavEntry(factoryOwner = activityFactoryOwner) {
                        RuleUpdateScreen(
                            ruleId = route.ruleId,
                            onNavigate = onNavigate
                        )
                    }
                }

                entry<Route.AlbumDetail> { route ->
                    HiltNavEntry(factoryOwner = activityFactoryOwner) {
                        AlbumDetailScreen(
                            albumId = route.albumId,
                            onNavigate = onNavigate
                        )
                    }
                }

                entry<Route.DirsRelation> { route ->
                    HiltNavEntry(factoryOwner = activityFactoryOwner) {
                        DirsRelationScreen(
                            albumId = route.albumId,
                            onNavigate = onNavigate
                        )
                    }
                }

                entry<Route.Sponsor> {
                    HiltNavEntry(factoryOwner = activityFactoryOwner) {
                        SponsorScreen(onNavigate = onNavigate)
                    }
                }

                entry<Route.StyleCreate> {
                    HiltNavEntry(factoryOwner = activityFactoryOwner) {
                        StyleCreateScreen(onNavigate = onNavigate)
                    }
                }

                entry<Route.StyleUpdate> { route ->
                    HiltNavEntry(factoryOwner = activityFactoryOwner) {
                        StyleUpdateScreen(
                            styleId = route.styleId,
                            onNavigate = onNavigate
                        )
                    }
                }
            }

            UpgradeDialog(viewModal = mainActivityViewModel, simpleSnackbar = simpleSnackbar)
        }
    }
}

@Composable
private fun HiltNavEntry(
    factoryOwner: HasDefaultViewModelProviderFactory,
    content: @Composable () -> Unit,
) {
    val entryStoreOwner = LocalViewModelStoreOwner.current!!
    val combinedOwner = remember(entryStoreOwner) {
        HiltEntryViewModelStoreOwner(entryStoreOwner, factoryOwner)
    }
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides combinedOwner,
        content = content,
    )
}

private class HiltEntryViewModelStoreOwner(
    storeOwner: ViewModelStoreOwner,
    private val factoryOwner: HasDefaultViewModelProviderFactory,
) : ViewModelStoreOwner by storeOwner, HasDefaultViewModelProviderFactory {
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = factoryOwner.defaultViewModelProviderFactory

    override val defaultViewModelCreationExtras: CreationExtras
        get() = factoryOwner.defaultViewModelCreationExtras
}