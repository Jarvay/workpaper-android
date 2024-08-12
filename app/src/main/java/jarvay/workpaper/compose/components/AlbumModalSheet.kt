package jarvay.workpaper.compose.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jarvay.workpaper.data.album.Album
import jarvay.workpaper.viewModel.AlbumListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumModalSheet(
    show: Boolean,
    defaultValues: List<Long> = emptyList(),
    onDismissRequest: () -> Unit,
    viewModal: AlbumListViewModel = hiltViewModel(),
    onCheckedChange: (List<Album>) -> Unit
) {
    val albumItemSize = 96.dp
    val albumList by viewModal.allAlbums.collectAsStateWithLifecycle()
    Log.d("albumList", albumList.toString())
    var checkedAlbumIds by remember {
        mutableStateOf(defaultValues)
    }

    fun emitChange() {
        val albums = albumList.filter { checkedAlbumIds.contains(it.albumId) }
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

    if (show) {
        ModalBottomSheet(onDismissRequest = onDismissRequest) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                itemsIndexed(
                    items = albumList,
                    key = { _, item -> item.albumId }) { _, album ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                updateCheckedIds(
                                    albumId = album.albumId,
                                    checked = checkedAlbumIds.contains(album.albumId)
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        AlbumItem(
                            album = album,
                            modifier = Modifier
                                .size(albumItemSize, albumItemSize)
                        )

                        val checked = checkedAlbumIds.contains(album.albumId)

                        if (!checked) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = null
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}