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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import jarvay.workpaper.others.MAX_PERSISTED_URI_GRANTS
import jarvay.workpaper.others.getSize
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.AlbumDetailViewModel

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
)
@Composable
fun AlbumDetailScreen(
    navController: NavController,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val simpleSnackbar = LocalSimpleSnackbar.current
    val album by viewModel.album.collectAsStateWithLifecycle()
    var selecting by remember {
        mutableStateOf(false)
    }
    var checkedState by remember {
        mutableStateOf(setOf<String>())
    }
    var deleteDialogShow by remember {
        mutableStateOf(false)
    }

    var currentWallpaper by remember {
        mutableStateOf("")
    }

    var actionsShow by remember {
        mutableStateOf(false)
    }

    if (album == null) return

    val context = LocalContext.current

    val listState = rememberLazyStaggeredGridState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            val takeFlags: Int =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            val splitUris = if (uris.size > MAX_PERSISTED_URI_GRANTS) {
                uris.subList(0, MAX_PERSISTED_URI_GRANTS - 1)
            } else {
                uris
            }

            val newUris = album!!.wallpaperUris.toMutableList()
            for (uri in splitUris) {
                if (newUris.contains(uri.toString())) continue
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                val permissions = context.contentResolver.persistedUriPermissions
                if (permissions.any { it.uri == uri }) {
                    newUris.add(uri.toString())
                }
            }

            viewModel.update(album!!.copy(wallpaperUris = newUris))
        }
    )

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            val takeFlags: Int =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                val persistedUriPermissions = context.contentResolver.persistedUriPermissions
                if (persistedUriPermissions.any { it.uri == uri }) {
                    val documentFile = DocumentFile.fromTreeUri(context, uri)
                        ?: return@rememberLauncherForActivityResult
                    val result = getImagesInDir(documentFile)
                    Log.d("result", result.toString())

                    var newUris = album!!.wallpaperUris.toMutableList()
                    newUris.addAll(result)
                    newUris = newUris.distinct().toMutableList()

                    viewModel.update(
                        album!!.copy(wallpaperUris = newUris)
                    )
                }
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
                    Text(album!!.name)
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
                        }) {
                        val uris =
                            album!!.wallpaperUris.filter { !checkedState.contains(it) }
                        Log.d("uris", uris.toString())
                        viewModel.update(album!!.copy(wallpaperUris = uris))
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
                                enabled = album!!.wallpaperUris.isNotEmpty()
                            )
                        } else {
                            DropdownMenuItem(text = {
                                Text(text = stringResource(id = R.string.select_all))
                            }, onClick = {
                                actionsShow = false
                                checkedState = album!!.wallpaperUris.toMutableSet()
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
            Text(
                text = stringResource(R.string.album_limit_tips),
                color = MaterialTheme.colorScheme.tertiary
            )

            LazyVerticalStaggeredGrid(
                state = listState,
                modifier = Modifier.padding(top = 16.dp),
                columns = StaggeredGridCells.Fixed(2),
            ) {
                items(items = album!!.wallpaperUris, key = { item ->
                    item
                }) {
                    Box(
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            val size = getSize(context, it)
                            val floatWidth = size.width.toFloat()
                            val floatHeight = size.height.toFloat()

                            AsyncImage(
                                model = ImageRequest.Builder(context).data(it.toUri())
                                    .size(320, 320)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .aspectRatio(floatWidth / floatHeight)
                                    .combinedClickable(onLongClick = {
                                        currentWallpaper = it
                                    }) {
                                        if (selecting) {
                                            val checked = !checkedState.contains(it)
                                            checkedState =
                                                updateCheckedState(checked, it, checkedState)
                                        }
                                    }
                            )
                        }

                        DropdownMenu(
                            expanded = currentWallpaper == it,
                            onDismissRequest = { currentWallpaper = "" }) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = R.string.album_set_as_cover)) },
                                onClick = {
                                    viewModel.update(album!!.copy(coverUri = it))
                                    currentWallpaper = ""
                                    simpleSnackbar.show(R.string.tips_operation_success)
                                }
                            )
                        }

                        if (selecting) {
                            Checkbox(
                                checked = checkedState.contains(it),
                                onCheckedChange = { checked ->
                                    checkedState = updateCheckedState(checked, it, checkedState)
                                },
                                modifier = Modifier.align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getImagesInDir(
    documentFile: DocumentFile,
    result: MutableList<String> = mutableListOf()
): MutableList<String> {
    for (item in documentFile.listFiles()) {
        if (item.isDirectory) {
            getImagesInDir(item, result)
        } else if (item.isFile) {
            val mimeType = item.type ?: continue
            if (!mimeType.startsWith("image/")) continue
            result.add(item.uri.toString())
        }
    }

    return result
}

private fun updateCheckedState(
    checked: Boolean,
    uri: String,
    checkedState: Set<String>
): MutableSet<String> {
    val checkedSet = checkedState.toMutableSet()
    if (checked) {
        checkedSet.add(uri)
    } else {
        checkedSet.remove(uri)
    }
    return checkedSet
}