package jarvay.workpaper.compose.album

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VideoFile
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.blankj.utilcode.util.LogUtils
import jarvay.workpaper.R
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.wallpaper.Wallpaper
import jarvay.workpaper.data.wallpaper.WallpaperType
import jarvay.workpaper.others.MAX_PERSISTED_URI_GRANTS
import jarvay.workpaper.others.PICKER_WALLPAPER_TYPES
import jarvay.workpaper.others.wallpaperType
import jarvay.workpaper.ui.theme.COLOR_BADGE_GREEN
import jarvay.workpaper.ui.theme.COLOR_BADGE_ORANGE
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.AlbumDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
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

    val listState = rememberLazyStaggeredGridState()

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

            val newWallpapers = mutableListOf<Wallpaper>()
            MainScope().launch {
                for (uri in splitUris) {
                    if (newWallpapers.find { it.contentUri == uri.toString() } != null) continue
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    val permissions = context.contentResolver.persistedUriPermissions
                    if (permissions.any { it.uri == uri }) {
                        val file = DocumentFile.fromSingleUri(context, uri)
                        file?.let {
                            newWallpapers.add(
                                Wallpaper(
                                    contentUri = uri.toString(),
                                    type = wallpaperType(it.type ?: ""),
                                    albumId = album.albumId
                                )
                            )
                        }
                    }
                }

                viewModel.addWallpapers(newWallpapers)
            }
        }
    )

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            simpleSnackbar.show(R.string.album_add_wallpaper_folder_tips)
            MainScope().launch(Dispatchers.IO) {
                viewModel.loading.value = true
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                if (uri != null) {
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    val documentFile = DocumentFile.fromTreeUri(context, uri)
                        ?: return@launch
                    viewModel.addWallpapersFromFolder(documentFile, albumId = album.albumId)
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
                                imagePickerLauncher.launch(PICKER_WALLPAPER_TYPES.toTypedArray())
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
                viewModel = viewModel,
                listState = listState,
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

@Composable
private fun WallpaperList(
    album: Album,
    wallpapers: List<Wallpaper>,
    checkedState: Set<Long>,
    selecting: Boolean,
    viewModel: AlbumDetailViewModel,
    listState: LazyStaggeredGridState,
    onItemCheckedChange: (Boolean, Long) -> Unit = { _: Boolean, _: Long -> }
) {


    LazyVerticalStaggeredGrid(
        state = listState,
        modifier = Modifier.padding(top = 16.dp),
        columns = StaggeredGridCells.Fixed(2),
    ) {
        items(items = wallpapers, key = { it.wallpaperId }) {
            WallpaperItem(
                wallpaper = it,
                album = album,
                viewModel = viewModel,
                selecting = selecting,
                checkedState = checkedState,
                onItemCheckedChange = onItemCheckedChange
            ) {
                if (selecting) {
                    val checked = !checkedState.contains(it.wallpaperId)
                    onItemCheckedChange(checked, it.wallpaperId)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WallpaperItem(
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper,
    album: Album,
    viewModel: AlbumDetailViewModel,
    selecting: Boolean,
    checkedState: Set<Long>,
    onItemCheckedChange: (Boolean, Long) -> Unit,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val simpleSnackbar = LocalSimpleSnackbar.current

    val contentUri = wallpaper.contentUri

    var dropMenuExpanded by remember {
        mutableStateOf(false)
    }

    val model = try {
        ImageRequest.Builder(context)
            .data(contentUri.toUri())
            .size(Size.ORIGINAL)
            .crossfade(true)
            .build()
    } catch (e: Exception) {
        LogUtils.e("AlbumDetailScreen", "Load wallpaper failed", e.toString())
        null
    }

    Box(
        modifier = modifier
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
            Box {
                SubcomposeAsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    loading = {
                        Icon(
                            imageVector = when (wallpaper.type) {
                                WallpaperType.IMAGE -> Icons.Default.Image
                                WallpaperType.VIDEO -> Icons.Default.VideoFile
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(1.0f)
                                .padding(4.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            onLongClick = {
                                dropMenuExpanded = true
                            },
                            onClick = onClick
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(color = Color.White.copy(alpha = 0.5F))
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp)
                    ) {
                        when (wallpaper.type) {
                            WallpaperType.IMAGE -> Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = COLOR_BADGE_ORANGE
                            )

                            WallpaperType.VIDEO -> Icon(
                                imageVector = Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = COLOR_BADGE_GREEN
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = dropMenuExpanded,
            onDismissRequest = { dropMenuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.album_set_as_cover)) },
                onClick = {
                    viewModel.update(album.copy(coverUri = contentUri))
                    dropMenuExpanded = false
                    simpleSnackbar.show(R.string.tips_operation_success)
                }
            )

            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.delete)) },
                onClick = {
                    viewModel.deleteWallpapers(listOf(wallpaper.wallpaperId))
                    dropMenuExpanded = false
                    simpleSnackbar.show(R.string.tips_operation_success)
                }
            )
        }

        if (selecting) {
            Checkbox(
                checked = checkedState.contains(wallpaper.wallpaperId),
                onCheckedChange = { checked ->
                    onItemCheckedChange(checked, wallpaper.wallpaperId)
                },
                modifier = Modifier.align(Alignment.TopEnd)
            )
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