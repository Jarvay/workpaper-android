package jarvay.workpaper.compose.style

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jarvay.workpaper.R
import jarvay.workpaper.compose.Route
import jarvay.workpaper.compose.components.CustomIconButton
import jarvay.workpaper.data.style.Style
import jarvay.workpaper.ui.theme.COLOR_FORM_LABEL
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Ok
import kotlin.math.roundToInt

@Composable
fun StyleForm(
    onNavigate: (Route) -> Unit,
    values: Style? = null,
    onSave: (Style) -> Unit
) {
    var style by remember {
        mutableStateOf(values ?: Style(name = ""))
    }

    Scaffold(topBar = {
        SmallTopAppBar(
            title = "",
            navigationIcon = {
                CustomIconButton(
                    imageVector = MiuixIcons.Back,
                    onClick = { onNavigate(Route.Home) })
            },
            actions = {
                val enable = style.name.isNotBlank() && style.name.isNotEmpty()

                CustomIconButton(
                    onClick = {
                        onSave(style.copy())
                    },
                    enabled = enable,
                    imageVector = MiuixIcons.Ok
                )
            }
        )
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StyleFormItem(label = stringResource(id = R.string.style_form_item_name)) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(id = R.string.style_form_item_name),
                    value = style.name, onValueChange = {
                        style = style.copy(name = it)
                    }
                )
            }

            StyleFormItemSlider(
                labelId = R.string.style_form_item_blur_radius,
                value = style.blurRadius,
                onValueChange = {
                    style = style.copy(blurRadius = it)
                },
                steps = 24,
                valueRange = 0f..25f
            )

            StyleFormItemSlider(
                labelId = R.string.style_form_item_noise,
                value = style.noisePercent,
                onValueChange = {
                    style = style.copy(noisePercent = it)
                },
                valueRange = 0f..100f
            )

            StyleFormItemSlider(
                labelId = R.string.style_form_item_brightness,
                value = style.brightness,
                onValueChange = {
                    style = style.copy(brightness = it)
                },
                valueRange = -100f..200f
            )

            StyleFormItemSlider(
                labelId = R.string.style_form_item_contrast,
                value = style.contrast,
                onValueChange = {
                    style = style.copy(contrast = it)
                },
                valueRange = -100f..200f
            )

            StyleFormItemSlider(
                labelId = R.string.style_form_item_saturation,
                value = style.saturation,
                onValueChange = {
                    style = style.copy(saturation = it)
                },
                valueRange = -100f..200f
            )
        }
    }
}

@Composable
private fun StyleFormItem(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.widthIn(96.dp),
            text = label,
            color = COLOR_FORM_LABEL
        )

        content()
    }
}

@Composable
private fun StyleFormItemSlider(
    @StringRes labelId: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    steps: Int = 0,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    StyleFormItem(label = stringResource(id = labelId)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomIconButton(onClick = {
                onValueChange(value - 1)
            }, enabled = value > valueRange.start.toInt()) {
                Icon(imageVector = Icons.Default.HorizontalRule, contentDescription = null)
            }

            Slider(
                modifier = Modifier
                    .weight(1f),
                value = value.toFloat(),
                onValueChange = {
                    onValueChange(it.roundToInt())
                },
                steps = steps,
                valueRange = valueRange,
                height = 24.dp
            )

            CustomIconButton(onClick = {
                onValueChange(value + 1)
            }, enabled = value < valueRange.endInclusive.toInt()) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }

            Text(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .widthIn(36.dp),
                text = value.toString()
            )
        }
    }
}