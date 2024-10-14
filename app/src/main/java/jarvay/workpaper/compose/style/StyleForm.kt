package jarvay.workpaper.compose.style

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.data.style.Style
import jarvay.workpaper.ui.theme.COLOR_FORM_LABEL
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleForm(
    navController: NavController,
    values: Style? = null,
    onSave: (Style) -> Unit
) {
    var style by remember {
        mutableStateOf(values ?: Style(name = ""))
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
                val enable = style.name.isNotBlank() && style.name.isNotEmpty()

                IconButton(
                    onClick = {
                        onSave(style.copy())
                    },
                    enabled = enable,
                ) {
                    Icon(Icons.Default.Save, "")
                }
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
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.style_form_item_name))
                    },
                    value = style.name, onValueChange = {
                        style = style.copy(name = it)
                    },
                    placeholder = {
                        Text(text = stringResource(id = R.string.style_form_item_name))
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
                valueRange = 0f..100f
            )

            StyleFormItemSlider(
                labelId = R.string.style_form_item_contrast,
                value = style.contrast,
                onValueChange = {
                    style = style.copy(contrast = it)
                },
                valueRange = 0f..100f
            )

            StyleFormItemSlider(
                labelId = R.string.style_form_item_saturation,
                value = style.saturation,
                onValueChange = {
                    style = style.copy(saturation = it)
                },
                valueRange = 0f..100f
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
            Slider(
                modifier = Modifier
                    .weight(1f),
                value = value.toFloat(),
                onValueChange = {
                    onValueChange(Math.round(it))
                },
                steps = steps,
                valueRange = valueRange
            )

            Text(
                modifier = Modifier.padding(start = 4.dp).widthIn(24.dp),
                text = value.toString()
            )
        }
    }
}