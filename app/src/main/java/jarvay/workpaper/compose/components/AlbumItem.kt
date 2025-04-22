package jarvay.workpaper.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.blankj.utilcode.util.LogUtils
import jarvay.workpaper.R
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.wallpaper.Wallpaper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumItem(
    modifier: Modifier = Modifier,
    album: Album,
    wallpapers: List<Wallpaper>,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current

    val cover = album.coverUri ?: wallpapers.getOrNull(0)?.contentUri

    val model = try {
        ImageRequest.Builder(context).data(cover).size(256, 256)
            .build()
    } catch (e: Exception) {
        LogUtils.e("AlbumItem", "Load album cover failed", e.toString())
        null
    }

    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = onClick
                )
                .fillMaxSize()
                .aspectRatio(1f),

            ) {
            if (cover != null) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxSize()
                )
            } else {
                Text(
                    text = stringResource(id = R.string.album_no_cover),
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color.LightGray.copy(alpha = 0.4F),
                    )
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    text = wallpapers.size.toString(),
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        color = Color.LightGray.copy(alpha = 0.4F),
                    ),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = album.name, color = Color.White)
            }
        }
    }
}