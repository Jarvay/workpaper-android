package jarvay.workpaper.compose.album

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.others.showToast
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.AlbumListViewModel

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AlbumListScreen(navController: NavController, viewModel: AlbumListViewModel = hiltViewModel()) {
    val lazyListState = rememberLazyGridState()

    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()

    var updateDialogShow by rememberSaveable {
        mutableStateOf(false)
    }

    var deleteDialogShow by remember {
        mutableStateOf(false)
    }

    var currentAlbum: Album? by remember {
        mutableStateOf(null)
    }
    val context = LocalContext.current

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        LazyVerticalGrid(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albums, key = { it.albumId }) {
                var itemMenuExpanded by remember {
                    mutableStateOf(false)
                }

                Box {
                    AlbumItem(album = it, modifier = Modifier.combinedClickable(onLongClick = {
                        currentAlbum = it
                        itemMenuExpanded = true
                    }) {
                        navController.navigate(Screen.AlbumDetail.createRoute(it.albumId))
                    })

                    DropdownMenu(
                        expanded = itemMenuExpanded,
                        onDismissRequest = { itemMenuExpanded = false }) {
                        DropdownMenuItem(text = {
                            Text(text = stringResource(id = R.string.edit))
                        }, onClick = {
                            Log.d("111111", "111111")
                            updateDialogShow = true
                            itemMenuExpanded = false
                        })
                        DropdownMenuItem(text = {
                            Text(text = stringResource(id = R.string.delete))
                        }, onClick = {
                            if (viewModel.isUsing(it.albumId)) {
                                showToast(context, R.string.album_is_using_tips)
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
        text = stringResource(R.string.album_wallpaper_delete_tips),
        show = deleteDialogShow,
        onDismissRequest = { deleteDialogShow = false }) {
        currentAlbum?.let {
            viewModel.delete(currentAlbum!!)
        }
    }

    AlbumUpdateDialog(show = updateDialogShow, album = currentAlbum) {
        updateDialogShow = false
    }
}

