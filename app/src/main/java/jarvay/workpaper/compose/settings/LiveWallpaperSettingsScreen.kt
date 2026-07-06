package jarvay.workpaper.compose.settings

import android.content.Context
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import kotlin.math.roundToInt
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
    var accessibilityDialogShow by remember {
        mutableStateOf(false)
    }

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
                        GestureEvent.entries.forEach { event ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = event.labelResId)) },
                                onClick = {
                                    if (event == GestureEvent.LOCK_SCREEN) {
                                        val accessibilityManager =
                                            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
                                        val enabledServices =
                                            accessibilityManager.getEnabledAccessibilityServiceList(
                                                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
                                            )
                                        val isAccessibilityEnabled =
                                            enabledServices.any { service ->
                                                service.resolveInfo.serviceInfo.packageName == context.packageName &&
                                                        service.resolveInfo.serviceInfo.name == jarvay.workpaper.service.LockAccessibilityService::class.java.name
                                            }

                                        if (!isAccessibilityEnabled) {
                                            accessibilityDialogShow = true
                                            gestureDropExpanded = false
                                            return@DropdownMenuItem
                                        }
                                    }

                                    viewModel.update(
                                        SettingsPreferencesKeys.DOUBLE_TAP_EVENT,
                                        event.name
                                    )
                                    gestureDropExpanded = false
                                })

                        }
                    }
                }
            }

            SettingsItem(labelId = R.string.settings_item_parallax_sensitivity) {
                Text(
                    text = "%",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = settings.parallaxSensitivity,
                onValueChange = { viewModel.update(SettingsPreferencesKeys.PARALLAX_SENSITIVITY, it) },
                onValueChangeFinished = {
                    val rounded = (settings.parallaxSensitivity * 10).roundToInt() / 10f
                    viewModel.update(SettingsPreferencesKeys.PARALLAX_SENSITIVITY, rounded)
                },
                valueRange = 0.1f..2.0f,
                steps = 18,
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            SettingsItem(labelId = R.string.settings_item_parallax_invert_direction) {
                Switch(
                    checked = settings.parallaxInvertDirection,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.PARALLAX_INVERT_DIRECTION, c)
                    })
            }

            SettingsItem(labelId = R.string.settings_item_parallax_frame_rate) {
                Text(
                    text = " fps",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = settings.parallaxFrameRate.toFloat(),
                onValueChange = { value ->
                    val options = intArrayOf(15, 30, 60, 90, 120)
                    val snapped = options.minByOrNull { option -> kotlin.math.abs(option - value.toInt()) } ?: 30
                    viewModel.update(SettingsPreferencesKeys.PARALLAX_FRAME_RATE, snapped)
                },
                onValueChangeFinished = {
                    val options = intArrayOf(15, 30, 60, 90, 120)
                    val current = settings.parallaxFrameRate
                    val snapped = options.minByOrNull { option -> kotlin.math.abs(option - current) } ?: 30
                    viewModel.update(SettingsPreferencesKeys.PARALLAX_FRAME_RATE, snapped)
                },
                valueRange = 15f..120f,
                steps = 3,
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            SettingsItem(labelId = R.string.settings_item_enable_depth_layers) {
                Switch(
                    checked = settings.enableDepthLayers,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.ENABLE_DEPTH_LAYERS, c)
                    })
            }

            if (settings.enableDepthLayers) {
                SettingsItem(labelId = R.string.settings_item_depth_strength) {
                    Text(
                        text = "%",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = settings.depthStrength,
                    onValueChange = { viewModel.update(SettingsPreferencesKeys.DEPTH_STRENGTH, it) },
                    onValueChangeFinished = {
                        val rounded = (settings.depthStrength * 20).roundToInt() / 20f
                        viewModel.update(SettingsPreferencesKeys.DEPTH_STRENGTH, rounded)
                    },
                    valueRange = 0.05f..0.5f,
                    steps = 8,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }

    SimpleDialog(
        text = stringResource(id = R.string.permission_request_device_admin),
        show = accessibilityDialogShow,
        onDismissRequest = { accessibilityDialogShow = false }) {
        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        accessibilityDialogShow = false
    }
}