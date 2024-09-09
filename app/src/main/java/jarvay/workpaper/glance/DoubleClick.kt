package jarvay.workpaper.glance

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DoubleClick(
    private val interval: Int = 200,
    private val onDouble: () -> Unit,
    private val onSingle: () -> Unit
) {
    private var lastTime: Long = 0

    fun click() {
        if (System.currentTimeMillis() - lastTime <= interval) {
            onDouble()
            lastTime = 0
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