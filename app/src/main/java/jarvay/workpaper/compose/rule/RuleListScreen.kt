package jarvay.workpaper.compose.rule

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.Screen
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.others.dayOptions
import jarvay.workpaper.others.formatTime
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.RuleListViewModel

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class
)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RuleListScreen(navController: NavController, viewModel: RuleListViewModel = hiltViewModel()) {
    val rules by viewModel.allRules.observeAsState(initial = emptyList())

    var itemMenuExpanded by remember {
        mutableStateOf(false)
    }
    var deleteDialogShow by remember {
        mutableStateOf(false)
    }
    var currentRule: Rule? = null

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = {
            navController.navigate(Screen.RuleCreate.route)
        }) {
            Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add))
        }
    }) { padding ->

        Log.d("padding", padding.toString())

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = rules, key = { item -> item.rule.ruleId }) {
                val rule = it.rule
                val album = it.album
                Card(
                    modifier = Modifier
                        .combinedClickable(onLongClick = {
                            currentRule = rule
                            itemMenuExpanded = true
                        }) {
                            navController.navigate(Screen.RuleUpdate.createRoute(it.rule.ruleId))
                        }
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val startTime = formatTime(rule.startHour, rule.startMinute)
                        RuleInfoItem(
                            labelId = R.string.rule_list_item_time,
                            value = startTime
                        )

                        RuleInfoItem(
                            labelId = R.string.rule_list_item_album,
                            value = album.name
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
                    }
                }

                DropdownMenu(
                    expanded = itemMenuExpanded,
                    onDismissRequest = { itemMenuExpanded = false }) {
                    DropdownMenuItem(text = {
                        Text(text = stringResource(id = R.string.delete))
                    }, onClick = {
                        deleteDialogShow = true
                        itemMenuExpanded = false
                    })
                }
            }
        }
    }

    SimpleDialog(
        show = deleteDialogShow,
        text = stringResource(id = R.string.rule_list_item_delete_tips),
        onDismissRequest = { deleteDialogShow = false }) {
        currentRule?.let {
            viewModel.deleteRule(currentRule!!)
        }
    }
}

@Composable
private fun RuleInfoItem(@StringRes labelId: Int, value: String) {
    val labelColor = Color(0xFF6D6D6D)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = stringResource(id = labelId), color = labelColor)
        Text(text = stringResource(id = R.string.symbol_colon), color = labelColor)
        Text(text = value)
    }
}