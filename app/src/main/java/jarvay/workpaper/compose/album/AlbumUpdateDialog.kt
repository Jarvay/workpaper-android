package jarvay.workpaper.compose.album

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import jarvay.workpaper.R
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.others.showToast
import jarvay.workpaper.viewModel.AlbumListViewModel

@Composable
fun AlbumUpdateDialog(
    show: Boolean,
    viewModel: AlbumListViewModel = hiltViewModel(),
    album: Album?,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    if (show) {
        AlbumFormDialog(onDismissRequest = onDismissRequest, onConfirm = { newName ->
            Log.d("album", album.toString())
            album?.let {
                if (viewModel.existsByName(newName)) {
                    showToast(context, R.string.album_name_exists)
                } else {
                    viewModel.update(album.copy(name = newName))
                    onDismissRequest()
                }
            }
        })
    }

}