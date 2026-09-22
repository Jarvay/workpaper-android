package jarvay.workpaper.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.blankj.utilcode.util.LogUtils
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.data.wallpaper.Wallpaper
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun AlbumItem(
    modifier: Modifier = Modifier,
    album: Album,
    wallpapers: List<Wallpaper>,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val colorScheme = MiuixTheme.colorScheme

    var cover = album.coverUri ?: wallpapers.getOrNull(0)?.contentUri

    if (album.hideCover) {
        cover = null
    }

    val model = try {
        ImageRequest.Builder(context).data(cover).size(256, 256).build()
    } catch (e: Exception) {
        LogUtils.e("AlbumItem", "Load album cover failed", e.toString())
        null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxSize(),
            pressFeedbackType = PressFeedbackType.Sink,
            onClick = onClick,
            onLongPress = onLongClick
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
            ) {
                val coverModifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))

                if (cover != null) {
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = coverModifier
                    )
                } else {
                    Icon(
                        imageVector = MiuixIcons.Image,
                        contentDescription = null,
                        modifier = coverModifier.scale(0.5f)
                    )
                }


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = album.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Badge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
            containerColor = colorScheme.primary,
        ) {
            Text(
                text = wallpapers.size.toString(),
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}