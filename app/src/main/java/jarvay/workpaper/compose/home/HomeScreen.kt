package jarvay.workpaper.compose.home

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jarvay.workpaper.R
import jarvay.workpaper.compose.Route
import jarvay.workpaper.compose.album.AlbumCreateDialog
import jarvay.workpaper.compose.album.AlbumListScreen
import jarvay.workpaper.compose.components.CustomIconButton
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.compose.rule.RuleListScreen
import jarvay.workpaper.compose.settings.SettingsScreen
import jarvay.workpaper.compose.style.StyleListScreen
import jarvay.workpaper.others.requestAlarmPermission
import jarvay.workpaper.viewModel.HomeScreenViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Background
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Weeks
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class WorkpaperPage(
    @param:StringRes val titleResId: Int, val iconImageVector: ImageVector
) {
    RULES(R.string.tab_title_rules, MiuixIcons.Weeks), ALBUMS(
        R.string.tab_title_albums,
        MiuixIcons.Album
    ),
    STYLES(
        R.string.tab_title_styles,
        MiuixIcons.Background
    ),
    SETTINGS(R.string.drawer_menu_settings, MiuixIcons.Settings),
}

@Composable
fun HomeScreen(
    onNavigate: (Route) -> Unit,
    pages: Array<WorkpaperPage> = WorkpaperPage.entries.toTypedArray(),
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current


    var alarmPermissionDialogShow by remember {
        mutableStateOf(false)
    }

    val runningPreferences by homeScreenViewModel.runningPreferences.collectAsStateWithLifecycle()
    val running = runningPreferences?.running ?: false

    ModalNavigationDrawer(
        drawerState = drawerState, drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    onNavigate = onNavigate,
                    drawerState = drawerState,
                )
            }
        }) {
        Scaffold(
            topBar = {
                TopBar(
                    drawerState = drawerState,
                    onNavigate = onNavigate,
                    pages = pages,
                    pagerState = pagerState
                )
            },
            bottomBar = {
                FloatingNavigationBar {
                    pages.withIndex().forEach { (index, page) ->
                        FloatingNavigationBarItem(
                            selected = pagerState.currentPage == pages.indexOf(page),
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pages.indexOf(page))
                                }
                            },
                            icon = page.iconImageVector,
                            label = stringResource(id = page.titleResId),
                        )

                        if (index == 1) {
                            FloatingActionButton(onClick = {
                                if (!running && checkPermissions(context, onRequestPermission = {
                                        alarmPermissionDialogShow = true
                                    })) {
                                    MainScope().launch {
                                        homeScreenViewModel.start()
                                    }

                                } else if (running) {
                                    MainScope().launch {
                                        homeScreenViewModel.stop()
                                    }
                                }
                            }) {
                                if (running) {
                                    Icon(
                                        imageVector = MiuixIcons.Pause,
                                        contentDescription = "",
                                        tint = Color.White,
                                    )
                                } else {
                                    Icon(
                                        imageVector = MiuixIcons.Play,
                                        contentDescription = "",
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }) { contentPadding ->
            HomePagerScreen(
                pagerState = pagerState,
                pages = pages,
                Modifier.padding(
                    top = contentPadding.calculateTopPadding(),
                ),
                onNavigate = onNavigate
            )

            SimpleDialog(
                show = alarmPermissionDialogShow,
                title = stringResource(id = R.string.permission_request_alarm),
                onDismissRequest = { alarmPermissionDialogShow = false }) {
                requestAlarmPermission(context = context)
            }
        }
    }
}

@Composable
fun HomePagerScreen(
    pagerState: PagerState,
    pages: Array<WorkpaperPage>,
    modifier: Modifier = Modifier,
    onNavigate: (Route) -> Unit
) {
    Column(modifier) {
        HorizontalPager(
            modifier = Modifier.background(MiuixTheme.colorScheme.background),
            state = pagerState,
            verticalAlignment = Alignment.Top,
        ) { index ->
            when (pages[index]) {
                WorkpaperPage.RULES -> {
                    RuleListScreen(onNavigate = onNavigate)
                }

                WorkpaperPage.ALBUMS -> {
                    AlbumListScreen(onNavigate = onNavigate)
                }

                WorkpaperPage.STYLES -> {
                    StyleListScreen(onNavigate = onNavigate)
                }

                WorkpaperPage.SETTINGS -> {
                    SettingsScreen(
                        onNavigate = onNavigate,
                    )
                }
            }
        }
    }
}

private fun checkPermissions(context: Context, onRequestPermission: () -> Unit): Boolean {
    var hasPermission = true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager: AlarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        hasPermission = alarmManager.canScheduleExactAlarms()

        if (!hasPermission) {
            onRequestPermission()
        }
    }

    return hasPermission
}

@Composable
private fun TopBar(
    drawerState: DrawerState,
    onNavigate: (Route) -> Unit,
    pages: Array<WorkpaperPage>,
    pagerState: PagerState,
) {
    val scope = rememberCoroutineScope()
    var albumCreateDialogShow by rememberSaveable {
        mutableStateOf(false)
    }


    SmallTopAppBar(navigationIcon = {
        CustomIconButton(onClick = {
            scope.launch {
                drawerState.open()
            }
        }, imageVector = Icons.Default.Menu, contentDescription = null)
    }, title = "", actions = {
        if (pages[pagerState.currentPage] == WorkpaperPage.SETTINGS) {
            return@SmallTopAppBar
        }

        CustomIconButton(
            imageVector = MiuixIcons.Add,
            contentDescription = stringResource(id = R.string.add),
            onClick = {
                when (pages[pagerState.currentPage]) {
                    WorkpaperPage.RULES -> {
                        onNavigate(Route.RuleCreate)
                    }

                    WorkpaperPage.ALBUMS -> {
                        albumCreateDialogShow = true
                    }

                    WorkpaperPage.STYLES -> {
                        onNavigate(Route.StyleCreate)
                    }

                    WorkpaperPage.SETTINGS -> {}
                }
            }
        )
    })

    AlbumCreateDialog(show = albumCreateDialogShow) {
        albumCreateDialogShow = false
    }
}