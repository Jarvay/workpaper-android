package jarvay.workpaper.compose.album

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.blankj.utilcode.util.LogUtils
import jarvay.workpaper.R
import jarvay.workpaper.compose.Route
import jarvay.workpaper.compose.components.CustomIconButton
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.wallpaper.Wallpaper
import jarvay.workpaper.data.wallpaper.WallpaperType
import jarvay.workpaper.others.MAX_PERSISTED_URI_GRANTS
import jarvay.workpaper.others.PICKER_WALLPAPER_TYPES
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.AlbumDetailViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    onNavigate: (Route) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel { factory: AlbumDetailViewModel.Factory ->
        factory.create(albumId)
    }
) {
    val context = LocalContext.current

    val simpleSnackbar = LocalSimpleSnackbar.current

    val albumWithWallpapers by viewModel.albumWithWallpapers.collectAsStateWithLifecycle()
    if (albumWithWallpapers == null) return

    val album = albumWithWallpapers!!.album
    val wallpapers = albumWithWallpapers!!.wallpapers

    val isRelatedMode = album.dirs?.isNotEmpty() ?: false
    val canRelateDir = wallpapers.isEmpty() || isRelatedMode

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

    var emptyDialogShow by remember { mutableStateOf(false) }

    var actionsShow by remember {
        mutableStateOf(false)
    }

    var pendingRoute by remember { mutableStateOf<Route?>(null) }

    var limitTipShow by remember {
        mutableStateOf(false)
    }
    var limitTipsContent by remember {
        mutableStateOf("")
    }

    val listState = rememberLazyStaggeredGridState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(), onResult = { uris: List<Uri> ->
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

            val splitUris = if (uris.size > MAX_PERSISTED_URI_GRANTS) {
                uris.subList(0, MAX_PERSISTED_URI_GRANTS - 1)
            } else {
                uris
            }

            viewModel.addFromUris(context = context, uris = splitUris)
        }
    )

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            simpleSnackbar.show(R.string.album_add_wallpaper_folder_tips)

            val takeFlags: Int =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                val documentFile = DocumentFile.fromTreeUri(context, uri)
                    ?: return@rememberLauncherForActivityResult
                viewModel.addWallpapersFromFolder(
                    context,
                    documentFile,
                )
            }
        }
    )

    BackHandler(enabled = selecting) {
        selecting = false
        checkedState = mutableSetOf()
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = album.name,
                navigationIcon = {
                    CustomIconButton(
                        imageVector = MiuixIcons.Back,
                        onClick = { onNavigate(Route.Home) })
                },
                actions = {
                    CustomIconButton(
                        imageVector = MiuixIcons.More,
                        onClick = { actionsShow = true })
                    SimpleDialog(
                        show = deleteDialogShow,
                        title = stringResource(id = R.string.album_wallpaper_delete_tips),
                        onDismissRequest = {
                            deleteDialogShow = false
                        }
                    ) {
                        viewModel.deleteWallpapers(checkedState.toList())
                        selecting = false
                        checkedState = emptySet()
                    }

                    OverlayListPopup(
                        show = actionsShow,
                        onDismissRequest = { actionsShow = false },
                        onDismissFinished = {
                            pendingRoute?.let { route ->
                                pendingRoute = null
                                onNavigate(route)
                            }
                        }
                    ) {
                        ListPopupColumn {
                            if (!selecting) {
                                if (!isRelatedMode) {
                                    Text(
                                        text = stringResource(id = R.string.album_add_images),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                actionsShow = false
                                                imagePickerLauncher.launch(PICKER_WALLPAPER_TYPES.toTypedArray())
                                            }
                                            .padding(horizontal = 20.dp, vertical = 12.dp)
                                    )

                                    Text(
                                        text = stringResource(id = R.string.album_add_folder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                actionsShow = false
                                                folderPickerLauncher.launch(null)
                                            }
                                            .padding(horizontal = 20.dp, vertical = 12.dp)
                                    )
                                }

                                if (canRelateDir) {
                                    Text(
                                        text = stringResource(id = R.string.album_relate_folders),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                pendingRoute = Route.DirsRelation(
                                                    albumId = album.albumId
                                                )
                                                actionsShow = false
                                            }
                                            .padding(horizontal = 20.dp, vertical = 12.dp)
                                    )
                                }

                                if (wallpapers.isNotEmpty()) {
                                    Text(
                                        text = stringResource(id = R.string.edit),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                actionsShow = false
                                                selecting = true
                                            }
                                            .padding(horizontal = 20.dp, vertical = 12.dp)
                                    )

                                    Text(
                                        text = stringResource(id = R.string.action_empty),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                actionsShow = false
                                                emptyDialogShow = true
                                            }
                                            .padding(horizontal = 20.dp, vertical = 12.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(id = R.string.select_all),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            actionsShow = false
                                            checkedState =
                                                wallpapers.map { it.wallpaperId }.toMutableSet()
                                        }
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                                )

                                Text(
                                    text = stringResource(id = R.string.cancel),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            actionsShow = false
                                            selecting = false
                                            checkedState = emptySet()
                                        }
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                                )

                                if (checkedState.isNotEmpty()) {
                                    Text(
                                        text = stringResource(id = R.string.delete),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                actionsShow = false
                                                deleteDialogShow = true
                                            }
                                            .padding(horizontal = 20.dp, vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (isRelatedMode) {
                    viewModel.updateWallpapersByDirs(context)
                } else {
                    actionsShow = true
                }
            }) {
                if (isRelatedMode) {
                    val infiniteTransition =
                        rememberInfiniteTransition(label = "infinite transition")
                    val rotate by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart),
                        label = "rotate"
                    )

                    Icon(
                        modifier = Modifier.graphicsLayer {
                            if (loading) {
                                rotationZ = rotate
                            }
                        },
                        imageVector = MiuixIcons.Refresh,
                        contentDescription = "",
                        tint = Color.White
                    )
                } else {
                    Icon(MiuixIcons.Add, contentDescription = stringResource(id = R.string.add))
                }
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

    SimpleDialog(
        title = stringResource(R.string.album_empty_tips),
        show = emptyDialogShow,
        onDismissRequest = { emptyDialogShow = false }) {
        emptyDialogShow = false
        viewModel.emptyAlbum()
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
    val colorScheme = MiuixTheme.colorScheme

    val contentUri = wallpaper.contentUri

    var dropMenuExpanded by remember {
        mutableStateOf(false)
    }

    val model = try {
        ImageRequest.Builder(context)
            .data(contentUri.toUri())
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .size(Size.ORIGINAL)
            .crossfade(true)
            .build()
    } catch (e: Exception) {
        LogUtils.e("AlbumDetailScreen", "Load wallpaper failed", e.toString())
        null
    }
    val ratio = wallpaper.ratio ?: 1.0f

    Box(
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .aspectRatio(ratio),
            pressFeedbackType = PressFeedbackType.Sink,
            onClick = onClick,
            onLongPress = {
                dropMenuExpanded = true
            }
        ) {
            Box {
                SubcomposeAsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "loading")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 1000),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )
                            Icon(
                                imageVector = when (wallpaper.type) {
                                    WallpaperType.IMAGE -> MiuixIcons.Image
                                    WallpaperType.VIDEO -> Icons.Default.VideoFile
                                },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer { this.alpha = alpha },
                                tint = colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(ratio)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 8.dp, y = 8.dp)
                        .size(24.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (wallpaper.type) {
                        WallpaperType.IMAGE -> Icon(
                            imageVector = MiuixIcons.Image,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )

                        WallpaperType.VIDEO -> Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (selecting) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            state = if (checkedState.contains(wallpaper.wallpaperId)) ToggleableState.On else ToggleableState.Off,
                            onClick = {
                                onItemCheckedChange(
                                    !checkedState.contains(wallpaper.wallpaperId),
                                    wallpaper.wallpaperId
                                )
                            }
                        )
                    }
                }
            }
        }

        OverlayListPopup(
            show = dropMenuExpanded,
            onDismissRequest = { dropMenuExpanded = false }
        ) {
            ListPopupColumn {
                Text(
                    text = stringResource(id = R.string.album_set_as_cover),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.update(album.copy(coverUri = contentUri))
                            dropMenuExpanded = false
                            simpleSnackbar.show(R.string.tips_operation_success)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )

                Text(
                    text = stringResource(id = R.string.delete),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.deleteWallpapers(listOf(wallpaper.wallpaperId))
                            dropMenuExpanded = false
                            simpleSnackbar.show(R.string.tips_operation_success)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
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