package jarvay.workpaper.compose.rule

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jarvay.workpaper.R
import jarvay.workpaper.compose.Route
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
import jarvay.workpaper.ui.theme.FORM_ITEM_SPACE
import jarvay.workpaper.viewModel.RuleFormViewModel
import jarvay.workpaper.viewModel.WorkpaperViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme


@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RuleForm(
    onNavigate: (Route) -> Unit,
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
    val styleOptions = styles.map { Pair(it.styleId, it.name) }.toMutableList().apply {
        add(0, Pair(-1, stringResource(R.string.rule_style_none)))
    }
    var selectedStyle by remember {
        mutableStateOf(value = values?.style)
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
            onDismiss = { startPickerShow = false }) { hour, minute ->
            rule = rule.copy(
                startHour = hour, startMinute = minute
            )
            startPickerShow = false
        }
    }


    Scaffold(topBar = {
        SmallTopAppBar(title = "", navigationIcon = {
            CustomIconButton(imageVector = MiuixIcons.Back, onClick = { onNavigate(Route.Home) })
        }, actions = {
            val saveEnable = selectedAlbums.isNotEmpty() && rule.days.isNotEmpty()

            CustomIconButton(onClick = {
                if (runningPreferences?.running == true) {
                    simpleSnackbar.show(R.string.tips_please_stop_first)
                    return@CustomIconButton
                }

                onSave(
                    rule.copy()
                )
            }, enabled = saveEnable, imageVector = MiuixIcons.Ok)
        })
    }) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(FORM_ITEM_SPACE),
            modifier = Modifier
                .padding(padding)
                .padding(bottom = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp), insideMargin = PaddingValues(16.dp)
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
                        Checkbox(
                            state = parentState, onClick = {
                                toggleAllChecked()
                            })
                        Spacer(modifier = Modifier.width(8.dp))
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
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        maxItemsInEachRow = 3
                    ) {
                        dayOptions.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(0.3f)
                                    .clickable {
                                        updateCheckedDays(
                                            !rule.days.contains(option.value), option.value
                                        )
                                    }) {
                                Checkbox(
                                    state = if (rule.days.contains(option.value)) ToggleableState.On else ToggleableState.Off,
                                    onClick = {
                                        updateCheckedDays(
                                            !rule.days.contains(option.value), option.value
                                        )
                                    })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = option.labelId))
                            }
                        }
                    }
                }


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.rule_start_time),
                        color = MiuixTheme.colorScheme.onBackground
                    )

                    TextField(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 24.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { startPickerShow = true },
                        label = stringResource(id = R.string.rule_start_time),
                        value = formatTime(rule.startHour, rule.startMinute),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                    )
                }
            }

            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                FlowRow(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Bottom),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    maxItemsInEachRow = 3
                ) {
                    val itemModifier =
                        Modifier
                            .width(88.dp)
                            .height(88.dp)
                            .weight(0.3f)
                            .aspectRatio(1f)
                            .fillMaxSize()
                            .fillMaxRowHeight(1f)

                    selectedAlbums.forEach {
                        AlbumItem(
                            album = it.album, wallpapers = it.wallpapers, modifier = itemModifier
                        ) {
                            onNavigate(Route.AlbumDetail(it.album.albumId))
                        }
                    }

                    Card(
                        modifier = itemModifier, onClick = {
                            albumModalSheetShow = true
                        }) {
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
            }

            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                CheckboxPreference(
                    title = stringResource(id = R.string.rule_no_style),
                    checked = rule.noStyle,
                    checkboxLocation = CheckboxLocation.End,
                    onCheckedChange = {
                        rule = rule.copy(
                            noStyle = it, styleId = if (it) rule.styleId else -1
                        )
                        if (it) {
                            selectedStyle = null
                        }
                    })

                if (!rule.noStyle) {
                    val styleNames = styles.map { it.name }.toMutableList()
                    if (styleNames.isNotEmpty()) {
                        styleNames.add(0, stringResource(id = R.string.rule_style_none))
                    }
                    OverlayDropdownPreference(
                        items = styleOptions.map { it.second },
                        selectedIndex = styleOptions.indexOfFirst { it.first == selectedStyle?.styleId },
                        title = stringResource(id = R.string.rule_style),
                        onSelectedIndexChange = { index ->
                            val selectedId = styleOptions[index].first
                            val selected = styles.find { it.styleId == selectedId }
                            selectedStyle = selected
                            rule = rule.copy(styleId = selectedId)
                        })
                }

                CheckboxPreference(
                    title = stringResource(id = R.string.rule_random),
                    checked = rule.random,
                    checkboxLocation = CheckboxLocation.End,
                    onCheckedChange = { rule = rule.copy(random = it) })

                CheckboxPreference(
                    title = stringResource(id = R.string.rule_change_by_timing),
                    checked = rule.changeByTiming,
                    checkboxLocation = CheckboxLocation.End,
                    onCheckedChange = { rule = rule.copy(changeByTiming = it) })

                if (rule.changeByTiming) {
                    NumberField(
                        label = stringResource(id = R.string.settings_item_interval),
                        value = rule.interval,
                        onValueChange = { interval -> rule = rule.copy(interval = interval) },
                        min = 1,
                        max = 24 * 60
                    )
                }

                CheckboxPreference(
                    title = stringResource(id = R.string.rule_change_while_unlock),
                    checked = rule.changeWhileUnlock,
                    checkboxLocation = CheckboxLocation.End,
                    onCheckedChange = { rule = rule.copy(changeWhileUnlock = it) })
            }
        }

        AlbumModalSheet(
            show = albumModalSheetShow,
            defaultValues = selectedAlbums.map { it.album.albumId },
            onDismissRequest = {
                albumModalSheetShow = false
            }) {
            selectedAlbums = it.toList()
            rule = rule.copy(
                albumIds = it.map { i -> i.album.albumId })
        }
    }
}