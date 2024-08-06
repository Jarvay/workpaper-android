package jarvay.workpaper.compose.album

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import jarvay.workpaper.R
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.others.showToast
import jarvay.workpaper.viewModel.AlbumListViewModel

@Composable
fun AlbumCreateDialog(
    show: Boolean,
    viewModel: AlbumListViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    if (show) {
        AlbumFormDialog(onDismissRequest = onDismissRequest, onConfirm = {
            if (viewModel.existsByName(it)) {
                showToast(context, R.string.album_name_exists)
            } else {
                viewModel.insert(Album(name = it))
                onDismissRequest()
            }
        })
    }
}