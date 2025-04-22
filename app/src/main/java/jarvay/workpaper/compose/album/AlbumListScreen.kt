package jarvay.workpaper.compose.album

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.Screen
import jarvay.workpaper.compose.components.AlbumItem
import jarvay.workpaper.compose.components.LocalMainActivityModel
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.album.AlbumWithWallpapers
import jarvay.workpaper.ui.theme.HOME_SCREEN_PAGER_VERTICAL_PADDING
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.AlbumListViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AlbumListScreen(
    navController: NavController,
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
                        navController.navigate(Screen.AlbumDetail.createRoute(it.album.albumId))
                    }

                    DropdownMenu(
                        expanded = itemMenuExpanded,
                        onDismissRequest = { itemMenuExpanded = false }) {
                        DropdownMenuItem(text = {
                            Text(text = stringResource(id = R.string.edit))
                        }, onClick = {
                            updateDialogShow = true
                            itemMenuExpanded = false
                        })
                        DropdownMenuItem(text = {
                            Text(text = stringResource(id = R.string.delete))
                        }, onClick = {
                            if (runningPreferences?.running == true) {
                                simpleSnackbar.show(R.string.tips_please_stop_first)
                                return@DropdownMenuItem
                            }

                            if (viewModel.isUsing(it.album.albumId)) {
                                simpleSnackbar.show(R.string.album_is_using_tips)
                            } else {
                                deleteDialogShow = true
                            }
                            itemMenuExpanded = false
                        })
                    }
                }
            }
        }
    }

    SimpleDialog(
        text = stringResource(R.string.album_delete_tips),
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

