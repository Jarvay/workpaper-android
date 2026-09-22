package jarvay.workpaper.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jarvay.workpaper.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun SimpleDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    dismissOnConfirm: Boolean = true,
    confirmButtonEnable: Boolean = true,
    confirmButtonText: String = stringResource(id = R.string.ok),
    dismissButtonText: String = stringResource(id = R.string.cancel),
    hideDismissButton: Boolean = false,
    title: String? = null,
    content: @Composable (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        OverlayDialog(
            show = true, title = title, onDismissRequest = onDismissRequest, content = {
                if (content != null) {
                    content()
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!hideDismissButton) {
                        TextButton(
                            modifier = Modifier.weight(0.5f),
                            text = dismissButtonText,
                            onClick = { onDismissRequest() })

                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    TextButton(
                        modifier = Modifier.weight(0.5f),
                        text = confirmButtonText,
                        enabled = confirmButtonEnable,
                        onClick = {
                            onConfirm()
                            if (dismissOnConfirm) {
                                onDismissRequest()
                            }
                        })
                }
            }, modifier = modifier
        )
    }
}

@Composable
fun SimpleDialog(
    text: String? = null,
    show: Boolean,
    dismissOnConfirm: Boolean = true,
    title: String? = null,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    val content = if (text != null) {
        @Composable {
            Text(text = text)
        }
    } else {
        null
    }

    SimpleDialog(
        show = show,
        title = title,
        content = content,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        dismissOnConfirm = dismissOnConfirm
    )
}