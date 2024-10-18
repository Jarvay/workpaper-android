package jarvay.workpaper.compose.settings

import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.preferences.SettingsPreferencesKeys
import jarvay.workpaper.others.requestNotificationPermission
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var notificationDialogShow by remember {
        mutableStateOf(false)
    }

    val simpleSnackbar = LocalSimpleSnackbar.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.drawer_menu_settings))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "")
                    }
                },
                actions = {}
            )
        },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxWidth()
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsItem(labelId = R.string.settings_item_start_with_prev_rule) {
                Switch(
                    checked = settings.startWithPrevRule,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.START_WITH_PREV_RULE, c)
                    })
            }

            SettingsItem(labelId = R.string.settings_item_also_set_lock_wallpaper) {
                Switch(
                    checked = settings.alsoSetLockWallpaper,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.ALSO_SET_LOCK_WALLPAPER, c)
                    })
            }

            SettingsItem(labelId = R.string.settings_item_hide_in_recent_task) {
                Switch(
                    checked = settings.hideInRecentTask,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.HIDE_IN_RECENT_TASK, c)
                    })
            }

            SettingsItem(labelId = R.string.settings_item_enable_dynamic_color) {
                Switch(
                    checked = settings.enableDynamicColor,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.ENABLE_DYNAMIC_COLOR, c)
                    })
            }

            SettingsItem(labelId = R.string.settings_item_disabled_when_playing_audio) {
                Switch(
                    checked = settings.disableWhenPlayingAudio,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.DISABLE_WHEN_PLAYING_AUDIO, c)
                    })
            }

            SettingsItem(labelId = R.string.settings_item_allow_wallpaper_scrolling) {
                Switch(
                    checked = settings.wallpaperScrollable,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.WALLPAPER_SCROLLABLE, c)
                    })
            }

            SettingsItem(labelId = R.string.settings_item_enable_notification) {
                Switch(
                    checked = settings.enableNotification,
                    onCheckedChange = { c ->
                        if (c) {
                            val hasPermission = checkNotifyPermission(context) {
                                notificationDialogShow = true
                            }
                            if (!hasPermission) return@Switch
                            viewModel.update(SettingsPreferencesKeys.ENABLE_NOTIFICATION, true)
                        } else {
                            viewModel.update(SettingsPreferencesKeys.ENABLE_NOTIFICATION, false)
                        }
                    })
            }

            if (settings.enableNotification) {
                SettingsItem(labelId = R.string.settings_item_notification_ongoing) {
                    Switch(
                        checked = settings.notificationOngoing,
                        onCheckedChange = { c ->
                            viewModel.update(SettingsPreferencesKeys.NOTIFICATION_ONGOING, c)
                        })
                }
            }

            SettingsItem(labelId = R.string.settings_item_use_live_wallpaper) {
                Switch(
                    checked = settings.useLiveWallpaper,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.USE_LIVE_WALLPAPER, c)
                        if (c) {
                            simpleSnackbar.show(R.string.settings_live_wallpaper_tips)
                        }
                    })
            }

            SettingsItem(labelId = R.string.settings_item_auto_check_update) {
                Switch(
                    checked = settings.autoCheckUpdate,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.AUTO_CHECK_UPDATE, c)
                    })
            }
        }

        SimpleDialog(
            text = stringResource(id = R.string.permission_request_notification),
            show = notificationDialogShow,
            onDismissRequest = { notificationDialogShow = false }) {
            requestNotificationPermission(context)
        }
    }
}

@Composable
private fun SettingsItem(@StringRes labelId: Int, content: @Composable () -> Unit) {
    SettingsItem(label = stringResource(id = labelId), content = content)
}

@Composable
private fun SettingsItem(label: String, content: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = label)

        content()
    }
}

private fun checkNotifyPermission(context: Context, onRequestPermission: () -> Unit): Boolean {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (notificationManager.areNotificationsEnabled()) {
        return true
    } else {
        onRequestPermission()
        return false
    }
}