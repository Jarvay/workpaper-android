package jarvay.workpaper.compose.album

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jarvay.workpaper.R

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

    AlertDialog(title = {
        Text(text = stringResource(id = R.string.album_name))
    }, onDismissRequest = onDismissRequest, confirmButton = {
        TextButton(onClick = { onConfirm(albumName) }, enabled = albumName.isNotEmpty()) {
            Text(text = stringResource(id = R.string.ok))
        }
    }, dismissButton = {
        TextButton(onClick = { onDismissRequest() }) {
            Text(text = stringResource(id = R.string.cancel))
        }
    }, text = {
        OutlinedTextField(
            value = albumName,
            onValueChange = {
                albumName = it
                onChange(it)
            },
            label = {
                Text(text = stringResource(id = R.string.album_name))
            },
            supportingText = {
                if (errorMessage != null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    })
}