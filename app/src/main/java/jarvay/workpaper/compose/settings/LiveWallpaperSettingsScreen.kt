package jarvay.workpaper.compose.settings

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.components.SettingsItem
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.preferences.SettingsPreferencesKeys
import jarvay.workpaper.others.GestureEvent
import jarvay.workpaper.others.deviceAdminIntent
import jarvay.workpaper.receiver.DeviceManagerReceiver
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveWallpaperSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var gestureDropExpanded by remember {
        mutableStateOf(false)
    }
    var deviceAdminDialogShow by remember {
        mutableStateOf(false)
    }

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.update(
                    SettingsPreferencesKeys.DOUBLE_TAP_EVENT,
                    GestureEvent.LOCK_SCREEN.name
                )
            }
        }
    )

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
                .verticalScroll(scrollState)
                .padding(it)
                .fillMaxWidth()
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsItem(labelId = R.string.settings_item_allow_wallpaper_scrolling) {
                Switch(
                    checked = settings.wallpaperScrollable,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.WALLPAPER_SCROLLABLE, c)
                    })
            }

            SettingsItem(labelId = R.string.settings_item_live_wallpaper_transition) {
                Switch(
                    checked = settings.imageTransition,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.IMAGE_TRANSITION, c)
                    })
            }

            SettingsItem(labelId = R.string.settings_item_video_wallpaper_reset_on_screen_off) {
                Switch(
                    checked = settings.videoResetProgressOnScreenOff,
                    onCheckedChange = { c ->
                        viewModel.update(
                            SettingsPreferencesKeys.VIDEO_RESET_PROGRESS_ON_SCREEN_OFF,
                            c
                        )
                    })
            }

            SettingsItem(labelId = R.string.settings_item_live_wallpaper_double_tap) {
                Box {
                    val labelId = try {
                        GestureEvent.valueOf(settings.doubleTapEvent)
                    } catch (e: Exception) {
                        GestureEvent.NONE
                    }.labelResId
                    Text(
                        modifier = Modifier.clickable {
                            gestureDropExpanded = true
                        },
                        text = stringResource(id = labelId),
                        color = MaterialTheme.colorScheme.primary
                    )

                    DropdownMenu(
                        expanded = gestureDropExpanded,
                        onDismissRequest = { gestureDropExpanded = false }) {
                        GestureEvent.entries.forEach {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = it.labelResId)) },
                                onClick = {
                                    val devicePolicyManager =
                                        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                    val isAdminActive = devicePolicyManager.isAdminActive(
                                        ComponentName(
                                            context,
                                            DeviceManagerReceiver::class.java
                                        )
                                    )
                                    if (it == GestureEvent.LOCK_SCREEN) {
                                        if (!isAdminActive) {
                                            deviceAdminDialogShow = true
                                            gestureDropExpanded = false
                                            return@DropdownMenuItem
                                        }
                                    } else {
                                        if (isAdminActive) {
                                            devicePolicyManager.removeActiveAdmin(
                                                ComponentName(
                                                    context,
                                                    DeviceManagerReceiver::class.java
                                                )
                                            )
                                        }
                                    }

                                    viewModel.update(
                                        SettingsPreferencesKeys.DOUBLE_TAP_EVENT,
                                        it.name
                                    )
                                    gestureDropExpanded = false
                                })
                        }
                    }
                }
            }
        }
    }

    SimpleDialog(
        text = stringResource(id = R.string.permission_request_device_admin),
        show = deviceAdminDialogShow,
        onDismissRequest = { deviceAdminDialogShow = false }) {
        deviceAdminLauncher.launch(deviceAdminIntent(context))
    }
}