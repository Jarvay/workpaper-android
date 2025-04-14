package jarvay.workpaper.compose.style

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.Screen
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.preferences.SettingsPreferencesKeys
import jarvay.workpaper.data.style.Style
import jarvay.workpaper.ui.theme.COLOR_BADGE_GREEN
import jarvay.workpaper.ui.theme.COLOR_LIST_LABEL
import jarvay.workpaper.ui.theme.COLOR_WHITE
import jarvay.workpaper.ui.theme.HOME_SCREEN_PAGER_VERTICAL_PADDING
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.StyleListViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun StyleListScreen(
    navController: NavController,
    viewModel: StyleListViewModel = hiltViewModel(),
) {
    val listState = rememberLazyListState()

    val styles by viewModel.allStyles.collectAsStateWithLifecycle()

    Scaffold { _ ->
        LazyColumn(
            modifier = Modifier
                .padding(
                    horizontal = SCREEN_HORIZONTAL_PADDING,
                    vertical = HOME_SCREEN_PAGER_VERTICAL_PADDING / 2
                ),
            state = listState,
        ) {
            items(styles, key = { it.styleId }) {
                StyleItem(
                    modifier = Modifier,
                    style = it,
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StyleItem(
    modifier: Modifier = Modifier,
    style: Style,
    viewModel: StyleListViewModel,
    navController: NavController,
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    var deleteDialogShow by remember {
        mutableStateOf(false)
    }

    val simpleSnackbar = LocalSimpleSnackbar.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier.padding(vertical = HOME_SCREEN_PAGER_VERTICAL_PADDING / 2)
    ) {
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            modifier = modifier.fillMaxSize()
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(onLongClick = {
                            expanded = true
                        }) {
                            navController.navigate(Screen.StyleUpdate.createRoute(style.styleId))
                        }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StyleInfoItem(
                        label = stringResource(id = R.string.style_list_item_name),
                        contentText = style.name
                    )

                    StyleInfoItem(
                        label = stringResource(id = R.string.style_list_item_blur_radius),
                        contentText = style.blurRadius.toString()
                    )

                    StyleInfoItem(
                        label = stringResource(id = R.string.style_list_item_noise),
                        contentText = style.noisePercent.toString()
                    )

                    StyleInfoItem(
                        label = stringResource(id = R.string.style_list_item_brightness),
                        contentText = style.brightness.toString()
                    )

                    StyleInfoItem(
                        label = stringResource(id = R.string.style_list_item_contrast),
                        contentText = style.contrast.toString()
                    )

                    StyleInfoItem(
                        label = stringResource(id = R.string.style_list_item_saturation),
                        contentText = style.saturation.toString()
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    if (settings.defaultStyleId == style.styleId) {
                        Badge(containerColor = COLOR_BADGE_GREEN) {
                            Text(
                                text = stringResource(id = R.string.style_list_item_default),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = COLOR_WHITE
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (settings.defaultStyleId != style.styleId) {
                        DropdownMenuItem(text = {
                            Text(text = stringResource(id = R.string.style_list_item_action_set_default))
                        }, onClick = {
                            viewModel.updateSettingsItem(
                                SettingsPreferencesKeys.DEFAULT_STYLE_ID,
                                style.styleId
                            )
                            expanded = false
                        })
                    } else {
                        DropdownMenuItem(text = {
                            Text(text = stringResource(id = R.string.style_list_item_action_cancel_default))
                        }, onClick = {
                            viewModel.updateSettingsItem(
                                SettingsPreferencesKeys.DEFAULT_STYLE_ID,
                                -1
                            )
                            expanded = false
                        })
                    }

                    DropdownMenuItem(text = {
                        Text(text = stringResource(id = R.string.delete))
                    }, onClick = {
                        deleteDialogShow = true
                        expanded = false
                    })
                }
            }
        }
    }

    SimpleDialog(
        show = deleteDialogShow,
        text = stringResource(id = R.string.rule_list_item_delete_tips),
        onDismissRequest = { deleteDialogShow = false }) {
        viewModel.delete(style)
        simpleSnackbar.show(R.string.tips_operation_success)
    }
}

@Composable
private fun StyleInfoItem(
    label: String,
    contentText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$label:", color = COLOR_LIST_LABEL)

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = contentText
        )
    }
}