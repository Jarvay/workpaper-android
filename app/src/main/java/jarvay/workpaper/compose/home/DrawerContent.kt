package jarvay.workpaper.compose.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import jarvay.workpaper.MainActivity
import jarvay.workpaper.R
import jarvay.workpaper.compose.Screen
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.others.download
import jarvay.workpaper.others.showToast
import jarvay.workpaper.viewModel.HomeScreenViewModel
import jarvay.workpaper.viewModel.MainActivityViewModel
import kotlinx.coroutines.launch

@Composable
fun DrawerContent(
    navController: NavController,
    drawerState: DrawerState,
    homeScreenViewModel: HomeScreenViewModel,
    mainActivityViewModel: MainActivityViewModel,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val upgradeDialogShow by homeScreenViewModel.upgradeDialogShow.observeAsState(initial = false)
    val checkingUpdate by homeScreenViewModel.checkingUpdate.observeAsState(initial = false)
    val latestVersion by homeScreenViewModel.latestVersion.observeAsState()

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

            NavigationDrawerItem(
                label = { Text(text = stringResource(id = R.string.drawer_menu_check_update)) },
                icon = {
                    if (checkingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = stringResource(
                                id = R.string.drawer_menu_check_update
                            )
                        )
                    }
                },
                selected = false,
                onClick = {
                    if (checkingUpdate) return@NavigationDrawerItem
                    homeScreenViewModel.checkUpdate()
                }
            )
        }
    }

    SimpleDialog(
        show = upgradeDialogShow,
        content = {
            Text(text = stringResource(id = R.string.tips_new_app_version))
        },
        onDismissRequest = {
            homeScreenViewModel.upgradeDialogShow.value = false
        }
    ) {
        latestVersion?.let {
            showToast(context, R.string.tips_start_downloading)
            val id = download(url = it.apkUrl, context = context)
            val intent = Intent()
            intent.setAction(MainActivity.ACTION_APK_DOWNLOAD_ID)
            intent.putExtra(MainActivity.APK_DOWNLOAD_ID_KEY, id)
            context.sendBroadcast(intent)
        }
    }
}