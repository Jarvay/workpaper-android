package jarvay.workpaper.compose.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jarvay.workpaper.R

@Composable
fun SimpleDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    dismissOnConfirm: Boolean = true,
    confirmButtonEnable: Boolean = true,
    content: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(onDismissRequest = { onDismissRequest() }, confirmButton = {
            TextButton(enabled = confirmButtonEnable, onClick = {
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
            content()
        }, modifier = modifier)
    }
}

@Composable
fun SimpleDialog(
    text: String,
    show: Boolean,
    dismissOnConfirm: Boolean = true,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    SimpleDialog(
        show = show, content = {
            Text(text = text)
        }, onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        dismissOnConfirm = dismissOnConfirm
    )
}