package jarvay.workpaper.compose.settings

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jarvay.workpaper.R
import jarvay.workpaper.compose.Route
import jarvay.workpaper.compose.components.CustomIconButton
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.preferences.SettingsPreferencesKeys
import jarvay.workpaper.others.GestureEvent
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun LiveWallpaperSettingsScreen(
    onNavigate: (Route) -> Unit, viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val simpleSnackbar = LocalSimpleSnackbar.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var accessibilityDialogShow by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(id = R.string.drawer_menu_settings),
                navigationIcon = {
                    CustomIconButton(
                        imageVector = MiuixIcons.Back,
                        onClick = { onNavigate(Route.Home) })
                },
                actions = {})
        },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(it)
                .fillMaxWidth()
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
        ) {
            Card {
                SwitchPreference(
                    title = stringResource(id = R.string.settings_item_use_live_wallpaper),
                    checked = settings.useLiveWallpaper,
                    onCheckedChange = { c ->
                        val activityManager = context.getSystemService(
                            ACTIVITY_SERVICE
                        ) as ActivityManager
                        val configInfo = activityManager.deviceConfigurationInfo
                        if (configInfo.reqGlEsVersion < 0x20000) {
                            simpleSnackbar.show(R.string.settings_opengles2_not_supported)
                            return@SwitchPreference
                        }

                        viewModel.update(SettingsPreferencesKeys.USE_LIVE_WALLPAPER, c)
                        if (c) {
                            simpleSnackbar.show(R.string.settings_live_wallpaper_tips)
                        }
                    })

                SwitchPreference(
                    title = stringResource(id = R.string.settings_item_allow_wallpaper_scrolling),
                    checked = settings.wallpaperScrollable,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.WALLPAPER_SCROLLABLE, c)
                    })

                SwitchPreference(
                    title = stringResource(id = R.string.settings_item_live_wallpaper_transition),
                    checked = settings.imageTransition,
                    onCheckedChange = { c ->
                        viewModel.update(SettingsPreferencesKeys.IMAGE_TRANSITION, c)
                    })

                SwitchPreference(
                    title = stringResource(id = R.string.settings_item_video_wallpaper_reset_on_screen_off),
                    checked = settings.videoResetProgressOnScreenOff,
                    onCheckedChange = { c ->
                        viewModel.update(
                            SettingsPreferencesKeys.VIDEO_RESET_PROGRESS_ON_SCREEN_OFF, c
                        )
                    })

                OverlayDropdownPreference(
                    title = stringResource(id = R.string.settings_item_live_wallpaper_double_tap),
                    items = GestureEvent.entries.map { stringResource(it.labelResId) },
                    selectedIndex = GestureEvent.entries.indexOfFirst { it.name == settings.doubleTapEvent }
                        .coerceAtLeast(0),
                    onSelectedIndexChange = { index ->
                        val event = GestureEvent.entries.getOrNull(index)
                            ?: return@OverlayDropdownPreference
                        if (event == GestureEvent.LOCK_SCREEN) {
                            val accessibilityManager =
                                context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
                            val enabledServices =
                                accessibilityManager.getEnabledAccessibilityServiceList(
                                    android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
                                )
                            val isAccessibilityEnabled = enabledServices.any { service ->
                                service.resolveInfo.serviceInfo.packageName == context.packageName && service.resolveInfo.serviceInfo.name == jarvay.workpaper.service.LockAccessibilityService::class.java.name
                            }

                            if (!isAccessibilityEnabled) {
                                accessibilityDialogShow = true
                                return@OverlayDropdownPreference
                            }
                        }

                        viewModel.update(
                            SettingsPreferencesKeys.DOUBLE_TAP_EVENT, event.name
                        )
                    })
            }
        }
    }

    SimpleDialog(
        title = stringResource(id = R.string.permission_request_device_admin),
        show = accessibilityDialogShow,
        onDismissRequest = { accessibilityDialogShow = false }) {
        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        accessibilityDialogShow = false
    }
}