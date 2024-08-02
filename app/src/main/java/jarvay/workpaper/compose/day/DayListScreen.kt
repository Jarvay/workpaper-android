package jarvay.workpaper.compose.day

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.Screen
import jarvay.workpaper.data.day.Day
import jarvay.workpaper.ui.theme.LIST_ITEM_HORIZONTAL_PADDING
import jarvay.workpaper.ui.theme.LIST_ITEM_VERTICAL_PADDING
import jarvay.workpaper.viewModel.DayListViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DayListScreen(navController: NavController, viewModel: DayListViewModel = hiltViewModel()) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Screen.DayCreate.route)
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add))
            }
        },
    ) { padding ->
        val dayList by viewModel.allDays.observeAsState(initial = emptyList())
        DayList(dayList, Modifier.padding(padding), navController = navController)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayList(days: List<Day>, modifier: Modifier = Modifier, navController: NavController) {
    LazyColumn(modifier = modifier) {
        items(items = days, key = { it.dayId }) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(
                    horizontal = LIST_ITEM_HORIZONTAL_PADDING,
                    vertical = LIST_ITEM_VERTICAL_PADDING
                ).clickable {
                    navController.navigate(Screen.DayDetail.createRoute(it.dayId))
                }.fillMaxWidth()
            ) {
                it.dayValues().forEach { dayValue ->
                    AssistChip(onClick = { /*TODO*/ }, label = {
                        Text(text = stringResource(id = dayValue.textId))
                    })
                }
                HorizontalDivider()
            }
        }
    }
}