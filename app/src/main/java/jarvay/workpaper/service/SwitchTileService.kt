package jarvay.workpaper.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import jarvay.workpaper.Workpaper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SwitchTileService : TileService() {
    @Inject
    lateinit var workpaper: Workpaper

    override fun onStartListening() {
        super.onStartListening()
        MainScope().launch {
            val running = workpaper.isRunning()
            qsTile.apply {
                state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        MainScope().launch {
            val running = workpaper.isRunning()
            qsTile.apply {
                state = if (!running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                updateTile()
            }

            if (!running) {
                workpaper.start()
            } else {
                workpaper.stop()
            }
        }
    }
}