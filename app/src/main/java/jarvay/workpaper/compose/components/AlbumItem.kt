package jarvay.workpaper.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jarvay.workpaper.R
import jarvay.workpaper.data.album.Album

@Composable
fun AlbumItem(
    modifier: Modifier = Modifier,
    album: Album,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current

    val cover = album.coverUri ?: album.wallpaperUris.getOrNull(0)

    Card(onClick = onClick, modifier = modifier) {
        Box(
            modifier = modifier
                .aspectRatio(1F)
        ) {
            if (cover != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(cover).size(256, 256)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.aspectRatio(1f)
                )
            } else {
                Text(
                    text = stringResource(id = R.string.album_no_cover), modifier = Modifier.align(
                        Alignment.Center
                    )
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