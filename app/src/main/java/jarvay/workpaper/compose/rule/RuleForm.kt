package jarvay.workpaper.compose.rule

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.Screen
import jarvay.workpaper.compose.components.AlbumItem
import jarvay.workpaper.compose.components.AlbumModalSheet
import jarvay.workpaper.compose.components.CustomIconButton
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.NumberField
import jarvay.workpaper.compose.components.TimePickerDialog
import jarvay.workpaper.data.rule.Rule
import jarvay.workpaper.data.rule.RuleWithRelation
import jarvay.workpaper.others.dayOptions
import jarvay.workpaper.others.formatTime
import jarvay.workpaper.ui.theme.COLOR_FORM_LABEL
import jarvay.workpaper.ui.theme.FORM_ITEM_SPACE
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.RuleFormViewModel
import jarvay.workpaper.viewModel.WorkpaperViewModel

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleForm(
    navController: NavController,
    values: RuleWithRelation? = null,
    viewModel: RuleFormViewModel,
    workpaperViewModel: WorkpaperViewModel = hiltViewModel(),
    onSave: (Rule) -> Unit,
) {
    val simpleSnackbar = LocalSimpleSnackbar.current

    val scrollState = rememberScrollState()

    val runningPreferences by workpaperViewModel.runningPreferences.collectAsStateWithLifecycle()

    var rule by remember {
        mutableStateOf(values?.rule ?: Rule())
    }

    var startPickerShow by remember {
        mutableStateOf(false)
    }
    var albumModalSheetShow by remember {
        mutableStateOf(false)
    }

    val styles by viewModel.styles.collectAsStateWithLifecycle()
    var selectedStyle by remember {
        mutableStateOf(value = values?.style)
    }
    var stylesExpanded by remember {
        mutableStateOf(false)
    }

    var selectedAlbums by remember {
        mutableStateOf(
            value = values?.albums ?: emptyList()
        )
    }

    val parentState = when {
        rule.days.size == dayOptions.size -> ToggleableState.On
        rule.days.isEmpty() -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }

    if (startPickerShow) {
        TimePickerDialog(
            hour = rule.startHour,
            minute = rule.startMinute,
            onDismiss = { startPickerShow = false }
        ) { timePickerState ->
            rule = rule.copy(
                startHour = timePickerState.hour,
                startMinute = timePickerState.minute
            )
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
                val saveEnable = selectedAlbums.isNotEmpty()
                        && rule.days.isNotEmpty()

                IconButton(onClick = {
                    if (runningPreferences?.running == true) {
                        simpleSnackbar.show(R.string.tips_please_stop_first)
                        return@IconButton
                    }

                    onSave(
                        rule.copy()
                    )
                }, enabled = saveEnable) {
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
                .padding(bottom = 16.dp)
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING)
                .verticalScroll(scrollState)
        ) {
            Column {
                fun toggleAllChecked() {
                    val newState = parentState != ToggleableState.On
                    val checkedDays = if (newState) {
                        dayOptions.map { it.value }
                    } else {
                        emptyList()
                    }
                    rule = rule.copy(days = checkedDays)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        toggleAllChecked()
                    }) {
                    TriStateCheckbox(
                        state = parentState,
                        onClick = {
                            toggleAllChecked()
                        }
                    )
                    Text(stringResource(id = R.string.select_all))
                }

                fun updateCheckedDays(checked: Boolean, dayValue: Int) {
                    val checkedDays = rule.days.toMutableList()
                    if (checked) {
                        checkedDays.add(dayValue)
                    } else {
                        checkedDays.remove(dayValue)
                    }
                    rule = rule.copy(days = checkedDays)
                }

                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 3
                ) {
                    dayOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(0.3f)
                                .clickable {
                                    updateCheckedDays(
                                        !rule.days.contains(option.value),
                                        option.value
                                    )
                                }
                        ) {
                            Checkbox(
                                checked = rule.days.contains(option.value),
                                onCheckedChange = { checked ->
                                    updateCheckedDays(checked, option.value)
                                })
                            Text(stringResource(id = option.labelId))
                        }
                    }
                }
            }


            RuleFormItem(
                labelId = R.string.rule_start_time
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .weight(0.8f),
                    label = {
                        Text(text = stringResource(id = R.string.rule_start_time))
                    },
                    value = formatTime(rule.startHour, rule.startMinute),
                    onValueChange = {},
                    readOnly = true,
                )

                CustomIconButton(
                    onClick = { startPickerShow = true },
                    modifier = Modifier.weight(0.2F, false)
                ) {
                    Icon(imageVector = Icons.Default.Timer, contentDescription = null)
                }
            }

            FlowRow(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Bottom),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                val itemModifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .weight(0.3f)
                    .aspectRatio(1f)
                    .fillMaxSize()
                    .fillMaxRowHeight(1f)

                selectedAlbums.forEach {
                    AlbumItem(
                        album = it.album,
                        wallpapers = it.wallpapers,
                        modifier = itemModifier
                    ) {
                        navController.navigate(Screen.AlbumDetail.createRoute(it.album.albumId))
                    }
                }

                Card(
                    modifier = itemModifier,
                    onClick = {
                        albumModalSheetShow = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        tint = Color.White
                    )
                }

                val placeholderCount = 3 - (selectedAlbums.size % 3)
                repeat(placeholderCount - 1) {
                    Column(modifier = itemModifier) {}
                }
            }

            RuleFormItem(
                labelId = R.string.rule_no_style
            ) {
                Checkbox(checked = rule.noStyle, onCheckedChange = {
                    rule = rule.copy(
                        noStyle = it,
                        styleId = if (it) -1 else rule.styleId
                    )
                    selectedStyle = null
                })
            }

            if (!rule.noStyle) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = defaultModifier,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(id = R.string.rule_style),
                            color = COLOR_FORM_LABEL
                        )

                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = selectedStyle?.name ?: ""
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (selectedStyle != null) {
                            TextButton(
                                modifier = Modifier.padding(end = 4.dp),
                                onClick = {
                                    selectedStyle = null
                                    rule = rule.copy(styleId = -1)
                                }
                            ) {
                                Text(text = stringResource(R.string.action_empty))
                            }
                        }

                        Box {
                            TextButton(onClick = {
                                stylesExpanded = true
                            }) {
                                Text(text = stringResource(R.string.select))
                            }

                            DropdownMenu(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                expanded = stylesExpanded,
                                onDismissRequest = { stylesExpanded = false }) {
                                styles.forEach {
                                    DropdownMenuItem(text = {
                                        Text(text = it.name)
                                    }, onClick = {
                                        rule = rule.copy(styleId = it.styleId)
                                        selectedStyle = it
                                        stylesExpanded = false
                                    })
                                }
                            }
                        }
                    }
                }
            }

            RuleFormItem(labelId = R.string.rule_random) {
                Switch(checked = rule.random, onCheckedChange = { rule = rule.copy(random = it) })
            }

            RuleFormItem(labelId = R.string.rule_change_by_timing) {
                Checkbox(
                    checked = rule.changeByTiming,
                    onCheckedChange = { rule = rule.copy(changeByTiming = it) })
            }

            if (rule.changeByTiming) {
                NumberField(
                    label = {
                        Text(
                            text = stringResource(id = R.string.settings_item_interval)
                        )
                    },
                    value = rule.interval,
                    onValueChange = { interval -> rule = rule.copy(interval = interval) },
                    min = 1,
                    max = 24 * 60
                )
            }

            RuleFormItem(labelId = R.string.rule_change_while_unlock) {
                Checkbox(
                    checked = rule.changeWhileUnlock,
                    onCheckedChange = { rule = rule.copy(changeWhileUnlock = it) })
            }
        }

        AlbumModalSheet(
            show = albumModalSheetShow,
            defaultValues = selectedAlbums.map { it.album.albumId },
            onDismissRequest = { albumModalSheetShow = false }
        ) {
            selectedAlbums = it.toList()
            rule = rule.copy(
                albumIds = it.map { i -> i.album.albumId }
            )
        }
    }
}

@Composable
private fun RuleFormItem(
    @StringRes labelId: Int,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = labelId),
            color = COLOR_FORM_LABEL
        )

        content()
    }
}