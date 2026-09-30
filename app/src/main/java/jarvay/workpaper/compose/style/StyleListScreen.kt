package jarvay.workpaper.compose.style

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jarvay.workpaper.R
import jarvay.workpaper.compose.Route
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.preferences.SettingsPreferencesKeys
import jarvay.workpaper.data.style.Style
import jarvay.workpaper.ui.theme.HOME_SCREEN_PAGER_PADDING_BOTTOM
import jarvay.workpaper.ui.theme.HOME_SCREEN_PAGER_VERTICAL_PADDING
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.StyleListViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType


@Composable
fun StyleListScreen(
    onNavigate: (Route) -> Unit,
    viewModel: StyleListViewModel = hiltViewModel(),
) {
    val listState = rememberLazyListState()

    val styles by viewModel.allStyles.collectAsStateWithLifecycle()

    Scaffold { _ ->
        LazyColumn(
            modifier = Modifier
                .padding(
                    horizontal = SCREEN_HORIZONTAL_PADDING,
                    vertical = HOME_SCREEN_PAGER_VERTICAL_PADDING / 2,
                ),
            contentPadding = PaddingValues(bottom = HOME_SCREEN_PAGER_PADDING_BOTTOM + HOME_SCREEN_PAGER_VERTICAL_PADDING / 2),
            state = listState,
        ) {
            items(styles, key = { it.styleId }) {
                StyleItem(
                    modifier = Modifier,
                    style = it,
                    viewModel = viewModel,
                    onNavigate = onNavigate
                )
            }
        }
    }
}

@Composable
private fun StyleItem(
    modifier: Modifier = Modifier,
    style: Style,
    viewModel: StyleListViewModel,
    onNavigate: (Route) -> Unit,
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    var deleteDialogShow by remember {
        mutableStateOf(false)
    }

    val simpleSnackbar = LocalSimpleSnackbar.current
    val colorScheme = MiuixTheme.colorScheme

    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier.padding(vertical = HOME_SCREEN_PAGER_VERTICAL_PADDING / 2)
    ) {
        Card(
            modifier = modifier.fillMaxSize(),
            pressFeedbackType = PressFeedbackType.Sink,
            onClick = {
                onNavigate(Route.StyleUpdate(style.styleId))
            },
            onLongPress = {
                expanded = true
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = style.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (settings.defaultStyleId == style.styleId) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.style_list_item_default),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ParamTag(
                            label = stringResource(id = R.string.style_list_item_blur_radius),
                            value = style.blurRadius.toString()
                        )
                        ParamTag(
                            label = stringResource(id = R.string.style_list_item_noise),
                            value = style.noisePercent.toString()
                        )
                        ParamTag(
                            label = stringResource(id = R.string.style_list_item_brightness),
                            value = style.brightness.toString()
                        )
                        ParamTag(
                            label = stringResource(id = R.string.style_list_item_contrast),
                            value = style.contrast.toString()
                        )
                        ParamTag(
                            label = stringResource(id = R.string.style_list_item_saturation),
                            value = style.saturation.toString()
                        )
                    }
                }
            }
        }

        OverlayListPopup(
            show = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ListPopupColumn {
                if (settings.defaultStyleId != style.styleId) {
                    Text(
                        text = stringResource(id = R.string.style_list_item_action_set_default),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateSettingsItem(
                                    SettingsPreferencesKeys.DEFAULT_STYLE_ID, style.styleId
                                )
                                expanded = false
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.style_list_item_action_cancel_default),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateSettingsItem(
                                    SettingsPreferencesKeys.DEFAULT_STYLE_ID, -1
                                )
                                expanded = false
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                Text(
                    text = stringResource(id = R.string.delete),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            deleteDialogShow = true
                            expanded = false
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }

    SimpleDialog(
        show = deleteDialogShow,
        title = stringResource(id = R.string.rule_list_item_delete_tips),
        onDismissRequest = { deleteDialogShow = false }) {
        viewModel.delete(style)
        simpleSnackbar.show(R.string.tips_operation_success)
    }
}

@Composable
private fun ParamTag(
    label: String,
    value: String
) {
    val colorScheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = colorScheme.onBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}