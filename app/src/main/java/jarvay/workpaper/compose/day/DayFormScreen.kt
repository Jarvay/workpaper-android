package jarvay.workpaper.compose.day

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import jarvay.workpaper.data.day.Day
import jarvay.workpaper.data.day.DayValue
import jarvay.workpaper.others.FormMode
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.DayListViewModel

data class DayOption(@StringRes val labelId: Int, val value: Int)

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayFormScreen(
    navController: NavController,
    formMode: FormMode = FormMode.CREATE,
    viewModel: DayListViewModel = hiltViewModel()
) {
    val dayOptions = listOf(
        DayOption(DayValue.MONDAY.textId, DayValue.MONDAY.day),
        DayOption(DayValue.TUESDAY.textId, DayValue.TUESDAY.day),
        DayOption(DayValue.WEDNESDAY.textId, DayValue.WEDNESDAY.day),
        DayOption(DayValue.THURSDAY.textId, DayValue.THURSDAY.day),
        DayOption(DayValue.FRIDAY.textId, DayValue.FRIDAY.day),
        DayOption(DayValue.SATURDAY.textId, DayValue.SATURDAY.day),
        DayOption(DayValue.SUNDAY.textId, DayValue.SUNDAY.day),
    )

    var checkedState by remember {
        mutableStateOf(mutableSetOf<Int>())
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = {
                Text("")
            },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "")
                }
            },
            actions = {
                if (checkedState.size > 0) IconButton(onClick = {
                    when (formMode) {
                        FormMode.CREATE -> {
                            viewModel.insert(Day(days = checkedState.toList()))
                            navController.navigateUp()
                        }

                        FormMode.UPDATE -> {}
                    }
                }) {
                    Icon(Icons.Default.Save, "")
                }
            }
        )
    }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(start = SCREEN_HORIZONTAL_PADDING)
        ) {
            dayOptions.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = option.labelId))
                    Checkbox(
                        checked = checkedState.contains(option.value),
                        onCheckedChange = { checked ->
                            val checkedSet = checkedState.toMutableSet()
                            if (checked) {
                                checkedSet.add(option.value)
                            } else {
                                checkedSet.remove(option.value)
                            }
                            checkedState = checkedSet.toMutableSet()
                        })
                }
            }
        }
    }
}