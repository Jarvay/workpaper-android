package jarvay.workpaper.compose.home

import android.util.Log
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import jarvay.workpaper.BuildConfig
import jarvay.workpaper.R
import jarvay.workpaper.compose.Screen
import jarvay.workpaper.compose.components.LocalMainActivityModel
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import kotlinx.coroutines.launch

@Composable
fun DrawerContent(
    navController: NavController,
    drawerState: DrawerState,
) {
    val simpleSnackbar = LocalSimpleSnackbar.current
    val mainActivityViewModel = LocalMainActivityModel.current

    val scope = rememberCoroutineScope()

    val checkingUpdate by mainActivityViewModel.checkingUpdate.observeAsState(initial = false)

    Log.d("BuildConfig.VERSION_NAME", BuildConfig.VERSION_NAME)

    Column(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
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
                        val infiniteTransition =
                            rememberInfiniteTransition(label = "infinite transition")
                        val rotate by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart),
                            label = "rotate"
                        )

                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = rotate
                            }
                        )
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
                    mainActivityViewModel.checkUpdate(
                        onError = {
                            simpleSnackbar.show(R.string.tips_check_update_failed)
                        }
                    ) {
                        if (!it) {
                            simpleSnackbar.show(R.string.tips_no_new_version)
                        }
                    }
                }
            )

            NavigationDrawerItem(
                label = { Text(text = stringResource(id = R.string.drawer_menu_sponsor)) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Money,
                        contentDescription = stringResource(
                            id = R.string.drawer_menu_sponsor
                        )
                    )
                },
                selected = false,
                onClick = {
                    navController.navigate(Screen.Sponsor.route)
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    }
}