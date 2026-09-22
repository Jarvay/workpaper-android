package jarvay.workpaper.compose.rule

import android.content.Intent
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import jarvay.workpaper.data.preferences.DEFAULT_SETTINGS
import jarvay.workpaper.data.preferences.SettingsPreferencesKeys
import jarvay.workpaper.data.rule.RuleWithRelation
import jarvay.workpaper.others.dayOptions
import jarvay.workpaper.others.formatTime
import jarvay.workpaper.receiver.RuleReceiver
import jarvay.workpaper.ui.theme.HOME_SCREEN_PAGER_PADDING_BOTTOM
import jarvay.workpaper.ui.theme.HOME_SCREEN_PAGER_VERTICAL_PADDING
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.RuleListViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType


@Composable
fun RuleListScreen(
    onNavigate: (Route) -> Unit,
    viewModel: RuleListViewModel = hiltViewModel(),
) {
    val ruleWithRelationList by viewModel.allRules.collectAsStateWithLifecycle()

    Scaffold { _ ->
        RuleList(
            ruleWithRelationList = ruleWithRelationList,
            viewModel = viewModel,
            onNavigate = onNavigate,
        )
    }
}

@Composable
private fun RuleList(
    modifier: Modifier = Modifier,
    ruleWithRelationList: List<RuleWithRelation>,
    viewModel: RuleListViewModel,
    onNavigate: (Route) -> Unit,
) {
    val listState = rememberLazyListState()

    val currentRuleId by viewModel.currentRuleId.collectAsStateWithLifecycle()
    val nextRuleId by viewModel.nextRuleId.collectAsStateWithLifecycle()

    val settingsPreferences by viewModel.settings.collectAsStateWithLifecycle()

    LazyColumn(
        state = listState,
        modifier = modifier
            .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
        contentPadding = PaddingValues(
            bottom = HOME_SCREEN_PAGER_PADDING_BOTTOM + HOME_SCREEN_PAGER_VERTICAL_PADDING / 2
        ),
        userScrollEnabled = true
    ) {
        items(
            items = ruleWithRelationList, key = { item -> item.rule.ruleId }) {
            val rule = it.rule

            RuleItem(
                modifier = Modifier,
                ruleWithRelation = it,
                isForced = settingsPreferences.forcedUsedRuleId == rule.ruleId,
                viewModel = viewModel,
                isCurrent = currentRuleId == rule.ruleId,
                isNext = nextRuleId == rule.ruleId,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun RuleItem(
    modifier: Modifier,
    ruleWithRelation: RuleWithRelation,
    isForced: Boolean,
    viewModel: RuleListViewModel,
    isCurrent: Boolean,
    isNext: Boolean,
    onNavigate: (Route) -> Unit,
) {
    val context = LocalContext.current
    val rule = ruleWithRelation.rule
    val albums = ruleWithRelation.albums
    val colorScheme = MiuixTheme.colorScheme

    var expanded by remember {
        mutableStateOf(false)
    }
    var deleteDialogShow by remember {
        mutableStateOf(false)
    }

    val simpleSnackbar = LocalSimpleSnackbar.current

    val runningPreferences by viewModel.runningPreferences.collectAsStateWithLifecycle()
    val running = runningPreferences?.running == true

    Row(
        modifier = Modifier.padding(vertical = HOME_SCREEN_PAGER_VERTICAL_PADDING / 2)
    ) {
        Card(
            modifier = modifier.fillMaxSize(),
            pressFeedbackType = PressFeedbackType.Sink,
            onClick = {
                onNavigate(Route.RuleUpdate(rule.ruleId))
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
                        imageVector = MiuixIcons.Timer,
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
                            text = formatTime(rule.startHour, rule.startMinute),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        RuleItemBadgesInline(
                            isForced = isForced,
                            isRunning = running,
                            isCurrent = isCurrent,
                            isNext = isNext,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RuleParamTag(
                            label = stringResource(id = R.string.rule_list_item_days),
                            value = rule.days.toIntArray().apply {
                                sort()
                            }.toList().mapNotNull { day ->
                                val option = dayOptions.find { opt -> opt.value == day }
                                if (option != null) stringResource(option.labelId) else null
                            }.joinToString(separator = ",")
                        )

                        RuleParamTag(
                            label = stringResource(id = R.string.rule_list_item_album),
                            value = albums.joinToString(separator = ", ") { item -> item.album.name }
                        )

                        if (ruleWithRelation.style != null) {
                            RuleParamTag(
                                label = stringResource(id = R.string.rule_list_item_style),
                                value = ruleWithRelation.style.name
                            )
                        }

                        if (rule.random) {
                            RuleParamTag(
                                label = "",
                                value = stringResource(id = R.string.rule_list_item_random)
                            )
                        }

                        if (rule.changeByTiming) {
                            RuleParamTag(
                                label = "",
                                value = stringResource(
                                    id = R.string.rule_list_item_interval, rule.interval
                                )
                            )
                        }

                        if (rule.changeWhileUnlock) {
                            RuleParamTag(
                                label = "",
                                value = stringResource(
                                    id = R.string.rule_list_item_change_when_unlock
                                )
                            )
                        }
                    }
                }
            }
        }

        OverlayListPopup(
            show = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ListPopupColumn {
                if (!isCurrent && running) {
                    Text(
                        text = stringResource(R.string.rule_list_item_apply_now),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val ruleIntent = Intent(context, RuleReceiver::class.java)
                                ruleIntent.putExtra(
                                    RuleReceiver.RULE_ID_KEY, rule.ruleId
                                )
                                context.sendBroadcast(ruleIntent)
                                expanded = false
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                Text(
                    text = stringResource(
                        id = if (isForced) {
                            R.string.rule_list_item_cancel_forced_used
                        } else {
                            R.string.rule_list_item_forced_used
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isForced) {
                                viewModel.updateSettingsItem(
                                    SettingsPreferencesKeys.FORCED_USED_RULE_ID,
                                    DEFAULT_SETTINGS.forcedUsedRuleId
                                )
                            } else {
                                viewModel.updateSettingsItem(
                                    SettingsPreferencesKeys.FORCED_USED_RULE_ID, rule.ruleId
                                )
                            }
                            expanded = false

                            if (running) {
                                MainScope().launch {
                                    viewModel.workpaper.restart()
                                }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )

                Text(
                    text = stringResource(id = R.string.delete),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (running) {
                                simpleSnackbar.show(R.string.tips_please_stop_first)
                                return@clickable
                            }

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
        viewModel.deleteRule(rule)
        simpleSnackbar.show(R.string.tips_operation_success)
    }
}

@Composable
private fun RuleItemBadgesInline(
    isForced: Boolean,
    isRunning: Boolean,
    isCurrent: Boolean,
    isNext: Boolean,
) {
    val colorScheme = MiuixTheme.colorScheme
    val textColor = Color.White

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        if (isForced) {
            Box(
                modifier = Modifier
                    .background(colorScheme.primary, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.rule_forced_apply),
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (!isRunning || isForced) return

        if (isCurrent) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF4CAF50), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.rule_current_rule),
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (isNext) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFFF9800), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.rule_next_rule),
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RuleParamTag(
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
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
        Text(
            text = value,
            color = colorScheme.onBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}