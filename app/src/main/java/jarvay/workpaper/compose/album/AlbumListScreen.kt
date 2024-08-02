package jarvay.workpaper.compose.album

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import jarvay.workpaper.R
import jarvay.workpaper.compose.Screen
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.others.showToast
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.AlbumListViewModel

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AlbumListScreen(navController: NavController, viewModel: AlbumListViewModel = hiltViewModel()) {
    val lazyListState = rememberLazyGridState()

    val albums by viewModel.allAlbums.observeAsState(initial = emptyList())

    var createDialogShow by rememberSaveable {
        mutableStateOf(false)
    }

    var deleteDialogShow by remember {
        mutableStateOf(false)
    }

    var currentAlbum: Album? = null
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    createDialogShow = true
                },
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add),
                )
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(albums, key = { it.albumId }) {
                val cover = it.coverUri
                    ?: (if (it.wallpaperUris.isNotEmpty()) it.wallpaperUris[0] else null)
                var itemMenuExpanded by remember {
                    mutableStateOf(false)
                }

                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Card(modifier = Modifier
                        .combinedClickable(onLongClick = {
                            currentAlbum = it
                            itemMenuExpanded = true
                        }) {
                            navController.navigate(Screen.AlbumDetail.createRoute(it.albumId))
                        }
                        .aspectRatio(1f)) {
                        if (cover != null) {
                            GlideImage(
                                model = cover, contentDescription = it.name,
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = it.name)
                    }
                }

                DropdownMenu(
                    expanded = itemMenuExpanded,
                    onDismissRequest = { itemMenuExpanded = false }) {
                    DropdownMenuItem(text = {
                        Text(text = stringResource(id = R.string.delete))
                    }, onClick = {
                        if (viewModel.isUsing(it.albumId)) {
                            showToast(context, R.string.album_is_using_tips)
                            itemMenuExpanded = false
                        } else {
                            deleteDialogShow = true
                            itemMenuExpanded = false
                        }
                    })
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

    if (createDialogShow) {
        AlbumFormDialog(onDismissRequest = {
            createDialogShow = false
        }, onConfirm = {
            viewModel.insert(Album(name = it))
            createDialogShow = false
        })
    }
}

@Composable
private fun AlbumFormDialog(onDismissRequest: () -> Unit, onConfirm: (String) -> Unit) {
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
        OutlinedTextField(value = albumName, onValueChange = { albumName = it }, label = {
            Text(text = stringResource(id = R.string.album_name))
        })
    })
}