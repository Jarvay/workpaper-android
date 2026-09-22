package jarvay.workpaper.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jarvay.workpaper.R
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TimePickerDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var hourValue by remember { mutableIntStateOf(hour) }
    var minuteValue by remember { mutableIntStateOf(minute) }

    OverlayDialog(
        show = true,
        title = stringResource(id = R.string.settings_item_timer),
        onDismissRequest = onDismiss,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    NumberPicker(
                        value = hourValue,
                        onValueChange = { hourValue = it },
                        modifier = Modifier.weight(1f),
                        range = 0..23,
                        visibleItemCount = 3,
                        label = { "%02d".format(it) }
                    )
                    Text(
                        text = ":",
                        style = MiuixTheme.textStyles.title1,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    NumberPicker(
                        value = minuteValue,
                        onValueChange = { minuteValue = it },
                        modifier = Modifier.weight(1f),
                        range = 0..59,
                        visibleItemCount = 3,
                        label = { "%02d".format(it) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        modifier = Modifier.weight(0.5f),
                        text = stringResource(id = R.string.cancel),
                        onClick = { onDismiss() }
                    )
                    Spacer(Modifier.width(16.dp))
                    TextButton(
                        modifier = Modifier.weight(0.5f),
                        text = stringResource(id = R.string.ok),
                        onClick = { onConfirm(hourValue, minuteValue) }
                    )
                }
            }
        }
    )
}