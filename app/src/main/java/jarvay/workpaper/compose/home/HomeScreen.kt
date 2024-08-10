package jarvay.workpaper.compose.home;

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.Screen
import jarvay.workpaper.compose.album.AlbumCreateDialog
import jarvay.workpaper.compose.album.AlbumListScreen
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.compose.rule.RuleListScreen
import jarvay.workpaper.others.requestAlarmPermission
import jarvay.workpaper.viewModel.HomeScreenViewModel
import kotlinx.coroutines.launch

enum class WorkpaperPage(
    @StringRes val titleResId: Int,
    val iconImageVector: ImageVector
) {
    RULES(R.string.tab_title_rules, Icons.AutoMirrored.Default.FormatListBulleted),
    ALBUMS(R.string.tab_title_albums, Icons.Default.PhotoAlbum)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    pages: Array<WorkpaperPage> = WorkpaperPage.entries.toTypedArray(),
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val scope = rememberCoroutineScope()

    var albumCreateDialogShow by rememberSaveable {
        mutableStateOf(false)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        NavigationDrawerItem(
                            label = { Text(text = stringResource(id = R.string.drawer_menu_settings)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(
                                        id = R.string.drawer_menu_settings
                                    )
                                )
                            },
                            selected = false,
                            onClick = {
                                navController.navigate(Screen.Settings.route)
                                scope.launch {
                                    drawerState.close()
                                }
                            }
                        )
                    }
                }
            }
        }) {
        Scaffold(topBar = {
            TopBar(drawerState = drawerState)
        }, floatingActionButton = {
            FloatingActionButton(onClick = {
                when (pages[pagerState.currentPage]) {
                    WorkpaperPage.RULES -> {
                        navController.navigate(Screen.RuleCreate.route)
                    }

                    WorkpaperPage.ALBUMS -> {
                        albumCreateDialogShow = true
                    }
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add))
            }
        }) { contentPadding ->
            HomePagerScreen(
                pagerState = pagerState,
                pages = pages,
                Modifier.padding(top = contentPadding.calculateTopPadding(), bottom = 16.dp),
                navController = navController
            )
        }
    }

    AlbumCreateDialog(show = albumCreateDialogShow) {
        albumCreateDialogShow = false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePagerScreen(
    pagerState: PagerState,
    pages: Array<WorkpaperPage>,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    Column(modifier) {
        val coroutineScope = rememberCoroutineScope()

        TabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            pages.forEachIndexed { index, page ->
                val title = stringResource(id = page.titleResId)
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(text = title) },
                    icon = {
                        Icon(imageVector = page.iconImageVector, contentDescription = title)
                    },
                    unselectedContentColor = MaterialTheme.colorScheme.secondary
                )
            }
        }

        HorizontalPager(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            state = pagerState,
            verticalAlignment = Alignment.Top,
        ) { index ->
            when (pages[index]) {
                WorkpaperPage.RULES -> {
                    RuleListScreen(navController = navController)
                }

                WorkpaperPage.ALBUMS -> {
                    AlbumListScreen(navController = navController)
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
        Log.d("checkPermissions", hasPermission.toString())

        if (!hasPermission) {
            onRequestPermission()
        }
    }

    return hasPermission
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    drawerState: DrawerState,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    var alarmPermissionDialogShow by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    val runningPreferences by homeScreenViewModel.runningPreferences.observeAsState()

    CenterAlignedTopAppBar(title = {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row {
                IconButton(onClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                }

                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.displaySmall
                )
            }

            Switch(checked = runningPreferences?.running ?: false, onCheckedChange = {
                if (it && checkPermissions(context, onRequestPermission = {
                        alarmPermissionDialogShow = true
                    })) {
                    homeScreenViewModel.start(scope)

                } else if (!it) {
                    homeScreenViewModel.stop(scope)
                }
            })
        }
    })

    SimpleDialog(
        show = alarmPermissionDialogShow,
        text = stringResource(id = R.string.permission_request_alarm),
        onDismissRequest = { alarmPermissionDialogShow = false }) {
        requestAlarmPermission(context = context)
    }
}
