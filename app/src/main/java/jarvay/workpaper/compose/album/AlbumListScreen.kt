package jarvay.workpaper.compose.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jarvay.workpaper.R
import jarvay.workpaper.compose.Route
import jarvay.workpaper.compose.components.AlbumItem
import jarvay.workpaper.compose.components.LocalMainActivityModel
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.album.AlbumWithWallpapers
import jarvay.workpaper.ui.theme.HOME_SCREEN_PAGER_PADDING_BOTTOM
import jarvay.workpaper.ui.theme.HOME_SCREEN_PAGER_VERTICAL_PADDING
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.AlbumListViewModel
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayListPopup


@Composable
fun AlbumListScreen(
    onNavigate: (Route) -> Unit,
    viewModel: AlbumListViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val mainActivityViewModel = LocalMainActivityModel.current
    val simpleSnackbar = LocalSimpleSnackbar.current
    val runningPreferences by mainActivityViewModel.runningPreferences.observeAsState()

    val lazyListState = rememberLazyGridState()
    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()

    var updateDialogShow by rememberSaveable {
        mutableStateOf(false)
    }

    var deleteDialogShow by remember {
        mutableStateOf(false)
    }

    var currentAlbumWithWallpapers: AlbumWithWallpapers? by remember {
        mutableStateOf(null)
    }


    Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
        LazyVerticalGrid(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = HOME_SCREEN_PAGER_VERTICAL_PADDING / 2)
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
            contentPadding = PaddingValues(bottom = HOME_SCREEN_PAGER_PADDING_BOTTOM + HOME_SCREEN_PAGER_VERTICAL_PADDING / 2),
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(albums, key = { it.album.albumId }) {
                var itemMenuExpanded by remember {
                    mutableStateOf(false)
                }

                Box(modifier = Modifier.padding(vertical = HOME_SCREEN_PAGER_VERTICAL_PADDING / 2)) {
                    AlbumItem(
                        album = it.album,
                        wallpapers = it.wallpapers,
                        onLongClick = {
                            currentAlbumWithWallpapers = it
                            itemMenuExpanded = true
                        },
                    ) {
                        onNavigate(Route.AlbumDetail(it.album.albumId))
                    }

                    OverlayListPopup(
                        show = itemMenuExpanded, onDismissRequest = { itemMenuExpanded = false }) {
                        ListPopupColumn {
                            Text(
                                text = stringResource(id = R.string.edit),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        updateDialogShow = true
                                        itemMenuExpanded = false
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp))
                            Text(
                                text = stringResource(
                                    id = if (it.album.hideCover) R.string.show_cover else R.string.hide_cover
                                ), modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.update(
                                            it.album.copy(hideCover = !it.album.hideCover)
                                        )
                                        itemMenuExpanded = false
                                        simpleSnackbar.show(R.string.tips_operation_success)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp))
                            Text(
                                text = stringResource(id = R.string.delete),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (runningPreferences?.running == true) {
                                            simpleSnackbar.show(R.string.tips_please_stop_first)
                                            return@clickable
                                        }

                                        if (viewModel.isUsing(it.album.albumId)) {
                                            simpleSnackbar.show(R.string.album_is_using_tips)
                                        } else {
                                            deleteDialogShow = true
                                        }
                                        itemMenuExpanded = false
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }

    SimpleDialog(
        title = stringResource(R.string.album_delete_tips),
        show = deleteDialogShow,
        onDismissRequest = { deleteDialogShow = false }) {
        currentAlbumWithWallpapers?.let {
            viewModel.delete(currentAlbumWithWallpapers!!, context)
            currentAlbumWithWallpapers = null
            simpleSnackbar.show(R.string.tips_operation_success)
        }
    }

    AlbumUpdateDialog(show = updateDialogShow, album = currentAlbumWithWallpapers?.album) {
        updateDialogShow = false
    }
}