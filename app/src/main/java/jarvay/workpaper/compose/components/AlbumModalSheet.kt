package jarvay.workpaper.compose.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jarvay.workpaper.data.album.AlbumWithWallpapers
import jarvay.workpaper.viewModel.AlbumListViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

@Composable
fun AlbumModalSheet(
    show: Boolean,
    defaultValues: List<Long> = emptyList(),
    onDismissRequest: () -> Unit,
    viewModal: AlbumListViewModel = hiltViewModel(),
    onCheckedChange: (List<AlbumWithWallpapers>) -> Unit
) {
    val albumItemSize = 96.dp
    val albumList by viewModal.allAlbums.collectAsStateWithLifecycle()

    var checkedAlbumIds by remember {
        mutableStateOf(defaultValues)
    }

    fun emitChange() {
        val albums = albumList.filter { checkedAlbumIds.contains(it.album.albumId) }
        onCheckedChange(albums)
    }

    fun updateCheckedIds(albumId: Long, checked: Boolean) {
        val newAlbumIds = checkedAlbumIds.toMutableList()
        if (checked) {
            newAlbumIds.remove(albumId)
        } else {
            newAlbumIds.add(albumId)
        }
        checkedAlbumIds = newAlbumIds.toSet().toList()
        emitChange()
    }

    BackHandler(enabled = show) {
        onDismissRequest()
    }

    OverlayBottomSheet(
        show = show, onDismissRequest = onDismissRequest
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(bottom = 16.dp)
                .height(320.dp)
        ) {
            itemsIndexed(
                items = albumList.toList(), key = { _, item -> item.album.albumId }) { _, item ->
                val album = item.album
                val wallpapers = item.wallpapers

                Card(onClick = {
                    updateCheckedIds(
                        albumId = album.albumId, checked = checkedAlbumIds.contains(album.albumId)
                    )
                }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        AlbumItem(
                            album = album,
                            wallpapers = wallpapers,
                            modifier = Modifier.size(albumItemSize, albumItemSize)
                        )

                        val checked = checkedAlbumIds.contains(album.albumId)
                        Checkbox(state = ToggleableState(checked), onClick = null)
                    }
                }
            }
        }
    }
}