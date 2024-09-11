package jarvay.workpaper.glance

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ClickHelper(
    private val interval: Int = 200,
    private val onDouble: () -> Unit,
    private val onSingle: () -> Unit
) {
    private var lastTime: Long = 0

    fun click() {
        if (System.currentTimeMillis() - lastTime <= interval) {
            lastTime = 0
            onDouble()
        } else {
            lastTime = System.currentTimeMillis()
            MainScope().launch {
                delay(interval.toLong())
                if (lastTime != 0L) {
                    onSingle()
                    lastTime = 0
                }
            }
        }
    }
}