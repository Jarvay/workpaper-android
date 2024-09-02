package jarvay.workpaper.compose.rule

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.Screen
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.others.dayOptions
import jarvay.workpaper.others.formatTime
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.RuleListViewModel

@OptIn(
    ExperimentalFoundationApi::class
)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RuleListScreen(
    navController: NavController,
    viewModel: RuleListViewModel = hiltViewModel(),
) {
    val rules by viewModel.allRules.collectAsStateWithLifecycle()
    val currentRuleAlbums by viewModel.currentRuleAlbums.collectAsStateWithLifecycle()
    val nextRuleAlbums by viewModel.nextRuleAlbums.collectAsStateWithLifecycle()

    Log.d("currentRuleAlbums", currentRuleAlbums.toString())

    val runningPreferences by viewModel.runningPreferences.collectAsStateWithLifecycle()

    var expandedMenuRuleId by remember {
        mutableLongStateOf(-1L)
    }
    var deleteDialogShow by remember {
        mutableStateOf(false)
    }
    var selectedRule: Rule? by remember {
        mutableStateOf(null)
    }
    val listState = rememberLazyListState()

    val simpleSnackbar = LocalSimpleSnackbar.current

    Scaffold { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING)
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = true
        ) {
            items(items = rules, key = { item -> item.rule.ruleId }) {
                val rule = it.rule
                val albums = it.albums

                Card(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .combinedClickable(onLongClick = {
                                    selectedRule = rule
                                    expandedMenuRuleId = it.rule.ruleId
                                }) {
                                    navController.navigate(Screen.RuleUpdate.createRoute(it.rule.ruleId))
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
                                value = albums.joinToString(separator = ", ") { album -> album.name }
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
                            expanded = expandedMenuRuleId == it.rule.ruleId,
                            onDismissRequest = { expandedMenuRuleId = -1 }) {
                            DropdownMenuItem(text = {
                                Text(text = stringResource(id = R.string.delete))
                            }, onClick = {
                                if (runningPreferences?.running == true) {
                                    simpleSnackbar.show(R.string.tips_please_stop_first)
                                    return@DropdownMenuItem
                                }

                                deleteDialogShow = true
                                expandedMenuRuleId = -1
                            })
                        }

                        if (runningPreferences?.running == true) {
                            Row(
                                modifier = Modifier
                                    .padding(end = 8.dp, top = 16.dp)
                                    .align(Alignment.TopEnd),
                                horizontalArrangement = Arrangement.spacedBy(
                                    4.dp,
                                    Alignment.CenterHorizontally
                                )
                            ) {
                                val textModifier =
                                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp)

                                val textColor = Color(0xFFFFFFFF)

                                if (currentRuleAlbums?.rule?.ruleId == it.rule.ruleId) {
                                    Badge(containerColor = Color(0xFF81C784)) {
                                        Text(
                                            text = stringResource(id = R.string.rule_current_rule),
                                            modifier = textModifier,
                                            color = textColor
                                        )
                                    }
                                }

                                if (nextRuleAlbums?.rule?.ruleId == it.rule.ruleId) {
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
                    }
                }
            }
        }

    }


    SimpleDialog(
        show = deleteDialogShow,
        text = stringResource(id = R.string.rule_list_item_delete_tips),
        onDismissRequest = { deleteDialogShow = false }) {
        Log.d("currentRule", selectedRule.toString())
        selectedRule?.let {
            viewModel.deleteRule(selectedRule!!)
            simpleSnackbar.show(R.string.tips_operation_success)
            selectedRule = null
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