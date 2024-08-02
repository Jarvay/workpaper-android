package jarvay.workpaper.data

import jarvay.workpaper.others.DEFAULT_WALLPAPER_CHANGE_INTERVAL

data class Settings(var startWithPrevRule: Boolean, var interval: Int)

val DEFAULT_SETTINGS =
    Settings(startWithPrevRule = true, interval = DEFAULT_WALLPAPER_CHANGE_INTERVAL)