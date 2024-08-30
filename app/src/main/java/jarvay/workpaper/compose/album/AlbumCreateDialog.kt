package jarvay.workpaper.compose.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import jarvay.workpaper.R
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.viewModel.AlbumListViewModel

@Composable
fun AlbumCreateDialog(
    show: Boolean,
    viewModel: AlbumListViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    if (show) {
        AlbumFormDialog(
            onDismissRequest = onDismissRequest,
            errorMessage = errorMessage,
            onChange = {
                if (errorMessage != null) {
                    errorMessage = ""
                }
            },
            onConfirm = {
                if (viewModel.existsByName(it)) {
                    errorMessage = context.getString(R.string.album_name_exists)
                } else {
                    viewModel.insert(Album(name = it))
                    onDismissRequest()
                }
            }
        )
    }
}