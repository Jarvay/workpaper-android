package jarvay.workpaper.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jarvay.workpaper.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AlbumFormDialog(
    onDismissRequest: () -> Unit,
    errorMessage: String? = null,
    onChange: (String) -> Unit = {},
    onConfirm: (String) -> Unit
) {
    var albumName by rememberSaveable {
        mutableStateOf("")
    }

    OverlayDialog(
        show = true,
        title = stringResource(id = R.string.album_name),
        onDismissRequest = onDismissRequest,
        content = {
            TextField(
                value = albumName,
                onValueChange = {
                    albumName = it
                    onChange(it)
                },
                label = stringResource(id = R.string.album_name),
                modifier = Modifier.padding(bottom = 16.dp),
            )
            if (errorMessage != null) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = errorMessage,
                    color = MiuixTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = stringResource(id = R.string.cancel),
                    onClick = { onDismissRequest() },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                TextButton(
                    text = stringResource(id = R.string.ok),
                    onClick = { onConfirm(albumName) },
                    enabled = albumName.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    )
}