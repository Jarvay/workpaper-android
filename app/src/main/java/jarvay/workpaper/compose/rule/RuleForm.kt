package jarvay.workpaper.compose.rule

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.components.CustomIconButton
import jarvay.workpaper.compose.components.NumberField
import jarvay.workpaper.compose.components.TimePickerDialog
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleWithAlbum
import jarvay.workpaper.others.DEFAULT_WALLPAPER_CHANGE_INTERVAL
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
    val parentState = when {
        checkedState.size == dayOptions.size -> ToggleableState.On
        checkedState.isEmpty() -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }

    var random by remember {
        mutableStateOf(values?.rule?.random ?: false)
    }
    var interval by remember {
        mutableIntStateOf(values?.rule?.interval ?: DEFAULT_WALLPAPER_CHANGE_INTERVAL)
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
                            interval = interval,
                            random = random,
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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TriStateCheckbox(
                        state = parentState,
                        onClick = {
                            val newState = parentState != ToggleableState.On
                            checkedState = if (newState) {
                                dayOptions.map { it.value }.toSet()
                            } else {
                                emptySet()
                            }
                        }
                    )
                    Text(stringResource(id = R.string.select_all))
                }

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
            }


            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = defaultModifier,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    label = {
                        Text(text = stringResource(id = R.string.rule_start_time))
                    },
                    value = formatTime(startTimePickerState.hour, startTimePickerState.minute),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(0.8F, true)
                )

                CustomIconButton(
                    onClick = { startPickerShow = true },
                    modifier = Modifier.weight(0.2F, false)
                ) {
                    Icon(imageVector = Icons.Default.Timer, contentDescription = null)
                }
            }

            Box(modifier = Modifier) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = defaultModifier,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        label = {
                            Text(text = stringResource(id = R.string.rule_album))
                        },
                        value = album?.name ?: "", onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(0.8F, true)
                    )

                    CustomIconButton(
                        onClick = { albumMenuExpanded = true },
                        modifier = Modifier.weight(0.2F, false),
                    ) {
                        Icon(imageVector = Icons.Default.PhotoAlbum, contentDescription = null)
                    }
                }
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = defaultModifier,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(id = R.string.rule_random))

                Switch(checked = random, onCheckedChange = { random = it })
            }

            NumberField(
                label = {
                    Text(
                        text = stringResource(id = R.string.settings_item_interval)
                    )
                },
                value = interval,
                onValueChange = { v ->
                    interval = v
                },
                min = 1,
                max = 24 * 60
            )
        }
    }
}