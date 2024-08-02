package jarvay.workpaper.compose.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jarvay.workpaper.R

@Composable
fun SimpleDialog(
    text: String,
    show: Boolean,
    dismissOnConfirm: Boolean = true,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(onDismissRequest = { onDismissRequest() }, confirmButton = {
            TextButton(onClick = {
                onConfirm()
                if (dismissOnConfirm) {
                    onDismissRequest()
                }
            }) {
                Text(text = stringResource(id = R.string.ok))
            }
        }, dismissButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }, text = {
            Text(text = text)
        })
    }
}