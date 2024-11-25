package jarvay.workpaper.others

import androidx.annotation.StringRes
import jarvay.workpaper.R

enum class FormMode {
    CREATE,
    UPDATE
}

enum class GestureEvent(@StringRes val labelResId: Int) {
    NONE(R.string.gesture_event_none),
    LOCK_SCREEN(R.string.gesture_event_lock_screen),
    CHANGE_WALLPAPER(R.string.gesture_event_change_wallpaper),
    OPEN_WECHAT(R.string.gesture_event_open_wechat),
    OPEN_WECHAT_SCAN(R.string.gesture_event_open_wechat_scan),
    OPEN_ALIPAY_SCAN(R.string.gesture_event_open_alipay_scan),
}