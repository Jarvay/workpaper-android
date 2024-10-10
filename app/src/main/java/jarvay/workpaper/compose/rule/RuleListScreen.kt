package jarvay.workpaper.compose.rule

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.Screen
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.preferences.DEFAULT_SETTINGS
import jarvay.workpaper.data.preferences.SettingsPreferencesKeys
import jarvay.workpaper.data.rule.RuleAlbums
import jarvay.workpaper.others.dayOptions
import jarvay.workpaper.others.formatTime
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.RuleListViewModel
import kotlinx.coroutines.launch

@Composable
fun RuleListScreen(
    navController: NavController,
    viewModel: RuleListViewModel = hiltViewModel(),
) {
    val ruleAlbumsList by viewModel.allRules.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        RuleList(
            modifier = Modifier.padding(paddingValues),
            ruleAlbumsList = ruleAlbumsList,
            viewModel = viewModel,
            navController = navController,
        )
    }
}

@Composable
private fun RuleList(
    modifier: Modifier,
    ruleAlbumsList: List<RuleAlbums>,
    viewModel: RuleListViewModel,
    navController: NavController,
) {
    val listState = rememberLazyListState()

    val currentRuleAlbums by viewModel.currentRuleAlbums.collectAsStateWithLifecycle()
    val nextRuleAlbums by viewModel.nextRuleAlbums.collectAsStateWithLifecycle()

    val settingsPreferences by viewModel.settings.collectAsStateWithLifecycle()

    LazyColumn(
        state = listState,
        modifier = modifier
            .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = true
    ) {
        items(items = ruleAlbumsList, key = { item -> item.rule.ruleId }) {
            val rule = it.rule

            RuleItem(
                modifier = Modifier,
                ruleAlbums = it,
                isForced = settingsPreferences.forcedUsedRuleId == rule.ruleId,
                viewModel = viewModel,
                isCurrent = currentRuleAlbums?.rule?.ruleId == rule.ruleId,
                isNext = nextRuleAlbums?.rule?.ruleId == rule.ruleId,
                navController = navController,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RuleItem(
    modifier: Modifier,
    ruleAlbums: RuleAlbums,
    isForced: Boolean,
    viewModel: RuleListViewModel,
    isCurrent: Boolean,
    isNext: Boolean,
    navController: NavController,
) {
    val rule = ruleAlbums.rule
    val albums = ruleAlbums.albums

    var expanded by remember {
        mutableStateOf(false)
    }
    var deleteDialogShow by remember {
        mutableStateOf(false)
    }

    val simpleSnackbar = LocalSimpleSnackbar.current

    val runningPreferences by viewModel.runningPreferences.collectAsStateWithLifecycle()
    val running = runningPreferences?.running == true

    Card(
        modifier = modifier
            .fillMaxSize()
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(onLongClick = {
                        expanded = true
                    }) {
                        navController.navigate(Screen.RuleUpdate.createRoute(rule.ruleId))
                    }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val startTime = formatTime(rule.startHour, rule.startMinute)
                RuleInfoItem(
                    labelId = R.string.rule_list_item_time,
                    value = startTime
                )

                RuleInfoItem(
                    labelId = R.string.rule_list_item_days,
                    value = rule.days.toIntArray().apply {
                        sort()
                    }.toList().mapNotNull { day ->
                        val option = dayOptions.find { opt -> opt.value == day }
                        if (option != null) stringResource(option.labelId) else null
                    }.joinToString(separator = ",")
                )

                RuleInfoItem(
                    labelId = R.string.rule_list_item_album,
                    value = albums.joinToString(separator = ", ") { item -> item.album.name }
                )

                if (rule.random) {
                    Text(text = stringResource(id = R.string.rule_list_item_random))
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (rule.changeByTiming) {
                        Text(
                            text = stringResource(
                                id = R.string.rule_list_item_interval,
                                rule.interval
                            )
                        )
                    }

                    if (rule.changeWhileUnlock) {
                        Text(
                            text = stringResource(
                                id = R.string.rule_list_item_change_when_unlock
                            )
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(
                                id = if (isForced) {
                                    R.string.rule_list_item_cancel_forced_used
                                } else {
                                    R.string.rule_list_item_forced_used
                                }
                            )
                        )
                    },
                    onClick = {
                        if (isForced) {
                            viewModel.updateSettingsItem(
                                SettingsPreferencesKeys.FORCED_USED_RULE_ID,
                                DEFAULT_SETTINGS.forcedUsedRuleId
                            )
                        } else {
                            viewModel.updateSettingsItem(
                                SettingsPreferencesKeys.FORCED_USED_RULE_ID,
                                rule.ruleId
                            )
                        }
                        expanded = false

                        if (running) {
                            viewModel.apply {
                                viewModelScope.launch {
                                    workpaper.restart()
                                }
                            }
                        }
                    }
                )

                DropdownMenuItem(text = {
                    Text(text = stringResource(id = R.string.delete))
                }, onClick = {
                    if (running) {
                        simpleSnackbar.show(R.string.tips_please_stop_first)
                        return@DropdownMenuItem
                    }

                    deleteDialogShow = true
                    expanded = false
                })
            }

            RuleItemBadges(
                modifier = Modifier.align(Alignment.TopEnd),
                isForced = isForced,
                isRunning = running,
                isCurrent = isCurrent,
                isNext = isNext,
            )
        }
    }

    SimpleDialog(
        show = deleteDialogShow,
        text = stringResource(id = R.string.rule_list_item_delete_tips),
        onDismissRequest = { deleteDialogShow = false }) {
        viewModel.deleteRule(rule)
        simpleSnackbar.show(R.string.tips_operation_success)
    }
}

@Composable
private fun RuleItemBadges(
    modifier: Modifier,
    isForced: Boolean,
    isRunning: Boolean,
    isCurrent: Boolean,
    isNext: Boolean,
) {
    val textModifier =
        Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    val textColor = Color(0xFFFFFFFF)


    Row(
        modifier = modifier
            .padding(end = 8.dp, top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(
            4.dp,
            Alignment.CenterHorizontally
        )
    ) {
        if (isForced) {
            Badge(containerColor = Color(0xFF81C784)) {
                Text(
                    text = stringResource(id = R.string.rule_forced_apply),
                    modifier = textModifier,
                    color = textColor
                )
            }
        }
        if (!isRunning) return

        if (isCurrent) {
            Badge(containerColor = Color(0xFF81C784)) {
                Text(
                    text = stringResource(id = R.string.rule_current_rule),
                    modifier = textModifier,
                    color = textColor
                )
            }
        }

        if (isNext) {
            Badge(containerColor = Color(0xFFFFB74D)) {
                Text(
                    text = stringResource(id = R.string.rule_next_rule),
                    modifier = textModifier,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun RuleInfoItem(@StringRes labelId: Int, value: String) {
    val labelColor = Color(0xFF6D6D6D)
    Row(verticalAlignment = Alignment.Top) {
        Text(text = stringResource(id = labelId), color = labelColor)
        Text(text = stringResource(id = R.string.symbol_colon), color = labelColor)
        Text(text = value)
    }
}