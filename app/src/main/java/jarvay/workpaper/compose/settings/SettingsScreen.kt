package jarvay.workpaper.compose.settings

import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jarvay.workpaper.R
import jarvay.workpaper.compose.Route
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.preferences.SettingsPreferencesKeys
import jarvay.workpaper.others.requestNotificationPermission
import jarvay.workpaper.request.REPO_MIRRORS_MAP
import jarvay.workpaper.ui.theme.HOME_SCREEN_PAGER_PADDING_BOTTOM
import jarvay.workpaper.ui.theme.HOME_SCREEN_PAGER_VERTICAL_PADDING
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference


@Composable
fun SettingsScreen(
    onNavigate: (Route) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var notificationDialogShow by remember {
        mutableStateOf(false)
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .navigationBarsPadding()
            .padding(bottom = HOME_SCREEN_PAGER_PADDING_BOTTOM + HOME_SCREEN_PAGER_VERTICAL_PADDING)
            .fillMaxWidth()
            .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card {
            ArrowPreference(
                title = stringResource(id = R.string.settings_item_live_wallpaper_mode),
                onClick = {
                    onNavigate(Route.LiveWallpaperSettings)
                })
        }

        Card {
            if (!settings.useLiveWallpaper) {
                SwitchPreference(
                    title = stringResource(id = R.string.settings_item_also_set_lock_wallpaper),
                    checked = settings.alsoSetLockWallpaper,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.ALSO_SET_LOCK_WALLPAPER, c)
                    })
            }

            SwitchPreference(
                title = stringResource(id = R.string.settings_item_start_with_prev_rule),
                checked = settings.startWithPrevRule,
                onCheckedChange = { c ->
                    viewModel.update(SettingsPreferencesKeys.START_WITH_PREV_RULE, c)
                })

            SwitchPreference(
                title = stringResource(id = R.string.settings_item_disabled_when_playing_audio),
                checked = settings.disableWhenPlayingAudio,
                onCheckedChange = { c ->
                    viewModel.update(SettingsPreferencesKeys.DISABLE_WHEN_PLAYING_AUDIO, c)
                })
        }

        Card {
            SwitchPreference(
                title = stringResource(id = R.string.settings_item_hide_in_recent_task),
                checked = settings.hideInRecentTask,
                onCheckedChange = { c ->
                    viewModel.update(SettingsPreferencesKeys.HIDE_IN_RECENT_TASK, c)
                })

            SwitchPreference(
                title = stringResource(id = R.string.settings_item_enable_dynamic_color),
                checked = settings.enableDynamicColor,
                onCheckedChange = { c ->
                    viewModel.update(SettingsPreferencesKeys.ENABLE_DYNAMIC_COLOR, c)
                })

            SwitchPreference(
                title = stringResource(id = R.string.settings_item_enable_notification),
                checked = settings.enableNotification,
                onCheckedChange = { c ->
                    if (c) {
                        val hasPermission = checkNotifyPermission(context) {
                            notificationDialogShow = true
                        }
                        if (!hasPermission) return@SwitchPreference
                        viewModel.update(SettingsPreferencesKeys.ENABLE_NOTIFICATION, true)
                    } else {
                        viewModel.update(SettingsPreferencesKeys.ENABLE_NOTIFICATION, false)
                    }
                })

            if (settings.enableNotification) {
                SwitchPreference(
                    title = stringResource(id = R.string.settings_item_notification_ongoing),
                    checked = settings.notificationOngoing,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.NOTIFICATION_ONGOING, c)
                    })
            }

            SwitchPreference(
                title = stringResource(id = R.string.settings_item_enable_log),
                checked = settings.enableLog,
                onCheckedChange = { c ->
                    viewModel.update(SettingsPreferencesKeys.ENABLE_LOG, c)
                })

            SwitchPreference(
                title = stringResource(id = R.string.settings_item_auto_check_update),
                checked = settings.autoCheckUpdate,
                onCheckedChange = { c ->
                    viewModel.update(SettingsPreferencesKeys.AUTO_CHECK_UPDATE, c)
                })

            OverlayDropdownPreference(
                title = stringResource(id = R.string.settings_item_repo_mirror),
                items = REPO_MIRRORS_MAP.keys.toList(),
                selectedIndex = REPO_MIRRORS_MAP.keys.indexOf(settings.repoMirror)
                    .coerceAtLeast(0),
                onSelectedIndexChange = { index ->
                    val key = REPO_MIRRORS_MAP.keys.elementAtOrNull(index)
                        ?: return@OverlayDropdownPreference
                    viewModel.update(SettingsPreferencesKeys.REPO_MIRROR, key)
                })
        }
    }

    SimpleDialog(
        title = stringResource(id = R.string.permission_request_notification),
        show = notificationDialogShow,
        onDismissRequest = { notificationDialogShow = false }) {
        requestNotificationPermission(context)
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