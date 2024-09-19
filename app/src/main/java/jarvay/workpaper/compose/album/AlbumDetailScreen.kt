package jarvay.workpaper.compose.album

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jarvay.workpaper.R
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.wallpaper.Wallpaper
import jarvay.workpaper.others.MAX_PERSISTED_URI_GRANTS
import jarvay.workpaper.others.getSize
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.AlbumDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    navController: NavController,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val simpleSnackbar = LocalSimpleSnackbar.current

    val albumWithWallpapers by viewModel.album.collectAsStateWithLifecycle()
    if (albumWithWallpapers == null) return

    val album = albumWithWallpapers!!.album
    val wallpapers = albumWithWallpapers!!.wallpapers

    val loading by viewModel.loading.collectAsStateWithLifecycle(initialValue = false)

    var selecting by remember {
        mutableStateOf(false)
    }
    var checkedState by remember {
        mutableStateOf(setOf<Long>())
    }
    var deleteDialogShow by remember {
        mutableStateOf(false)
    }

    var actionsShow by remember {
        mutableStateOf(false)
    }

    var limitTipShow by remember {
        mutableStateOf(false)
    }
    var limitTipsContent by remember {
        mutableStateOf("")
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            val existsCount = context.contentResolver.persistedUriPermissions.size
            val leaveCount = MAX_PERSISTED_URI_GRANTS - existsCount
            val after = existsCount + uris.size
            if (after > MAX_PERSISTED_URI_GRANTS) {
                limitTipsContent = context.getString(
                    R.string.album_limit_tips,
                    MAX_PERSISTED_URI_GRANTS,
                    existsCount,
                    leaveCount,
                    uris.size
                )
                limitTipShow = true
                return@rememberLauncherForActivityResult
            }

            val takeFlags: Int =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            val splitUris = if (uris.size > MAX_PERSISTED_URI_GRANTS) {
                uris.subList(0, MAX_PERSISTED_URI_GRANTS - 1)
            } else {
                uris
            }

            val newUris = wallpapers.map { it.contentUri }.toMutableList()
            for (uri in splitUris) {
                if (newUris.contains(uri.toString())) continue
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                val permissions = context.contentResolver.persistedUriPermissions
                if (permissions.any { it.uri == uri }) {
                    newUris.add(uri.toString())
                }
            }

            viewModel.addWallpapers(newUris)
        }
    )

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            simpleSnackbar.show(R.string.album_add_wallpaper_folder_tips)
            scope.launch(Dispatchers.IO) {
                viewModel.loading.value = true
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                if (uri != null) {
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    val documentFile = DocumentFile.fromTreeUri(context, uri)
                        ?: return@launch
                    viewModel.addWallpapersFromFolder(documentFile)
                }
                viewModel.loading.value = false
            }
        }
    )

    BackHandler(enabled = selecting) {
        selecting = false
        checkedState = mutableSetOf()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(album.name)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "")
                    }
                },
                actions = {
                    IconButton(onClick = { actionsShow = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                    }

                    SimpleDialog(
                        show = deleteDialogShow,
                        text = stringResource(id = R.string.album_wallpaper_delete_tips),
                        onDismissRequest = {
                            deleteDialogShow = false
                        }
                    ) {
                        viewModel.deleteWallpapers(checkedState.toList())
                        selecting = false
                        checkedState = emptySet()
                    }

                    DropdownMenu(
                        expanded = actionsShow,
                        onDismissRequest = { actionsShow = false }) {
                        if (!selecting) {
                            DropdownMenuItem(text = {
                                Text(text = stringResource(id = R.string.album_add_images))
                            }, onClick = {
                                actionsShow = false
                                imagePickerLauncher.launch(arrayOf("image/*"))
                            })

                            DropdownMenuItem(text = {
                                Text(text = stringResource(id = R.string.album_add_folder))
                            }, onClick = {
                                actionsShow = false
                                folderPickerLauncher.launch(null)
                            })

                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(id = R.string.edit))
                                }, onClick = {
                                    actionsShow = false
                                    selecting = true
                                },
                                enabled = wallpapers.isNotEmpty()
                            )
                        } else {
                            DropdownMenuItem(text = {
                                Text(text = stringResource(id = R.string.select_all))
                            }, onClick = {
                                actionsShow = false
                                checkedState = wallpapers.map { it.wallpaperId }.toMutableSet()
                            })

                            DropdownMenuItem(text = {
                                Text(text = stringResource(id = R.string.cancel))
                            }, onClick = {
                                actionsShow = false
                                selecting = false
                                checkedState = emptySet()
                            })

                            DropdownMenuItem(text = {
                                Text(text = stringResource(id = R.string.delete))
                            }, onClick = {
                                actionsShow = false
                                deleteDialogShow = true
                            }, enabled = checkedState.isNotEmpty())
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                actionsShow = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING)
        ) {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            WallpaperList(
                album = album,
                wallpapers = wallpapers,
                checkedState = checkedState,
                selecting = selecting,
                viewModel = viewModel
            ) { checked, uri ->
                checkedState =
                    updateCheckedState(checked, uri, checkedState)
            }
        }
    }

    SimpleDialog(
        content = {
            Text(text = limitTipsContent)
        },
        show = limitTipShow,
        hideDismissButton = true,
        confirmButtonText = stringResource(id = R.string.close),
        onDismissRequest = { limitTipShow = false }
    ) {
        limitTipShow = false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WallpaperList(
    album: Album,
    wallpapers: List<Wallpaper>,
    checkedState: Set<Long>,
    selecting: Boolean,
    viewModel: AlbumDetailViewModel,
    onItemCheckedChange: (Boolean, Long) -> Unit = { _: Boolean, _: Long -> }
) {
    val context = LocalContext.current

    val simpleSnackbar = LocalSimpleSnackbar.current
    val listState = rememberLazyStaggeredGridState()

    var currentWallpaper by remember {
        mutableStateOf<Wallpaper?>(null)
    }

    LazyVerticalStaggeredGrid(
        state = listState,
        modifier = Modifier.padding(top = 16.dp),
        columns = StaggeredGridCells.Fixed(2),
    ) {
        items(items = wallpapers, key = { item ->
            item.wallpaperId
        }) {
            val contentUri = it.contentUri

            val model = try {
                ImageRequest.Builder(context)
                    .data(contentUri.toUri())
                    .size(256, 256)
                    .build()
            } catch (e: Exception) {
                Log.w("AlbumDetailScreen", e.toString())
                null
            }

            Box(
                modifier = Modifier.animateItemPlacement()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .apply {
                            if (model == null) {
                                height(256.dp)
                            }
                        }
                ) {
                    val size = getSize(context, contentUri) ?: return@Card
                    val floatWidth = size.width.toFloat()
                    val floatHeight = size.height.toFloat()

                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(floatWidth / floatHeight)
                            .combinedClickable(onLongClick = {
                                currentWallpaper = it
                            }) {
                                if (selecting) {
                                    val checked = !checkedState.contains(it.wallpaperId)
                                    onItemCheckedChange(checked, it.wallpaperId)
                                }
                            }
                    )
                }

                DropdownMenu(
                    expanded = currentWallpaper == it,
                    onDismissRequest = { currentWallpaper = null }) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.album_set_as_cover)) },
                        onClick = {
                            viewModel.update(album.copy(coverUri = contentUri))
                            currentWallpaper = null
                            simpleSnackbar.show(R.string.tips_operation_success)
                        }
                    )
                }

                if (selecting) {
                    Checkbox(
                        checked = checkedState.contains(it.wallpaperId),
                        onCheckedChange = { checked ->
                            onItemCheckedChange(checked, it.wallpaperId)
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

private fun updateCheckedState(
    checked: Boolean,
    wallpaperId: Long,
    checkedState: Set<Long>
): MutableSet<Long> {
    val checkedSet = checkedState.toMutableSet()
    if (checked) {
        checkedSet.add(wallpaperId)
    } else {
        checkedSet.remove(wallpaperId)
    }
    return checkedSet
}