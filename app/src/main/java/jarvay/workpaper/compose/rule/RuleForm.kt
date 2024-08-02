package jarvay.workpaper.compose.rule

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.components.TimePickerDialog
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleWithAlbum
import jarvay.workpaper.others.FormMode
import jarvay.workpaper.others.dayOptions
import jarvay.workpaper.others.formatTime
import jarvay.workpaper.ui.theme.FORM_ITEM_SPACE
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleForm(
    navController: NavController,
    albums: List<Album>,
    values: RuleWithAlbum? = null,
    onSave: (Rule) -> Unit,
) {
    var startTimePickerState = rememberTimePickerState(
        is24Hour = true,
        initialHour = values?.rule?.startHour ?: 0,
        initialMinute = values?.rule?.startMinute ?: 0
    )

    var startPickerShow by remember {
        mutableStateOf(false)
    }
    var albumMenuExpanded by remember {
        mutableStateOf(false)
    }
    var album by remember {
        mutableStateOf(value = values?.album)
    }

    var checkedState by remember {
        mutableStateOf(values?.rule?.days?.toSet() ?: dayOptions.map { it.value }.toSet())
    }

    if (startPickerShow) {
        TimePickerDialog(
            timePickerState = startTimePickerState,
            onDismiss = { startPickerShow = false }) { timePickerState ->
            startTimePickerState = timePickerState
            startPickerShow = false
        }
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
                IconButton(onClick = {
                    onSave(
                        Rule(
                            startHour = startTimePickerState.hour,
                            startMinute = startTimePickerState.minute,
                            albumId = album!!.albumId,
                            days = checkedState.toList()
                        )
                    )
                }, enabled = album != null) {
                    Icon(Icons.Default.Save, "")
                }
            }
        )
    }) { padding ->
        val defaultModifier = Modifier.fillMaxWidth()

        Column(
            verticalArrangement = Arrangement.spacedBy(FORM_ITEM_SPACE),
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING)
        ) {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 96.dp)) {
                items(items = dayOptions) { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Text(stringResource(id = option.labelId))
                    }
                }
            }


            OutlinedTextField(
                label = {
                    Text(text = stringResource(id = R.string.rule_start_time))
                },
                value = formatTime(startTimePickerState.hour, startTimePickerState.minute),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                modifier = defaultModifier.clickable {
                    startPickerShow = true
                })

            Box(modifier = Modifier) {
                OutlinedTextField(
                    label = {
                        Text(text = stringResource(id = R.string.rule_album))
                    },
                    value = album?.name ?: "", onValueChange = {},
                    modifier = defaultModifier.clickable {
                        albumMenuExpanded = true
                    },
                    enabled = false,
                )
                DropdownMenu(
                    expanded = albumMenuExpanded,
                    onDismissRequest = { albumMenuExpanded = false }) {
                    albums.forEach {
                        DropdownMenuItem(text = {
                            Text(text = it.name)
                        }, onClick = {
                            album = it
                            albumMenuExpanded = false
                        })
                    }
                }
            }
        }
    }
}