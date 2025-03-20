package jarvay.workpaper.compose.album

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.blankj.utilcode.util.LogUtils
import jarvay.workpaper.R
import jarvay.workpaper.compose.components.SimpleDialog
import jarvay.workpaper.others.getOneWallpaperInDir
import jarvay.workpaper.ui.theme.COLOR_FORM_LABEL
import jarvay.workpaper.ui.theme.SCREEN_HORIZONTAL_PADDING
import jarvay.workpaper.viewModel.AlbumDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirsRelationScreen(
    navController: NavController, viewModel: AlbumDetailViewModel = hiltViewModel()
) {

    val context = LocalContext.current

    val album by viewModel.album.collectAsStateWithLifecycle()
    if (album == null) return

    val dirs = album!!.dirs ?: emptyList()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                viewModel.addDir(uri.toString())
            }
        })

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
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                folderPickerLauncher.launch(null)
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = true
        ) {
            items(items = dirs, key = { it }) {
                DirItem(
                    fullPath = it, viewModel = viewModel, context = context
                )
            }
        }
    }
}

@Composable
fun DirItem(fullPath: String, viewModel: AlbumDetailViewModel, context: Context) {
    val coverSize = 96.dp

    var deleteDialogShow by remember {
        mutableStateOf(false)
    }

    val uri = fullPath.toUri()
    val coverFile = context.getOneWallpaperInDir(uri) ?: return

    val model = try {
        ImageRequest.Builder(context).data(coverFile.uri).size(256, 256).build()
    } catch (e: Exception) {
        LogUtils.e("AlbumItem", "Load album cover failed", e.toString())
        null
    }

    val dirsList = uri.lastPathSegment?.split("/") ?: emptyList()
    val dir = dirsList.last()

    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(coverSize)
                        .height(coverSize)
                        .aspectRatio(1f)
                )
            }
            Text(
                text = dir, color = COLOR_FORM_LABEL
            )

            IconButton(onClick = {
                deleteDialogShow = true
            }) {
                Icon(Icons.Default.DeleteForever, "")
            }
        }
    }

    SimpleDialog(text = stringResource(R.string.album_related_folder_delete_tips),
        show = deleteDialogShow,
        onDismissRequest = { deleteDialogShow = false }) {
        viewModel.removeDir(fullPath)
    }
}