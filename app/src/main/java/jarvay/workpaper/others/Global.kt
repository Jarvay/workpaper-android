package jarvay.workpaper.others

import kotlinx.coroutines.CoroutineScope
import top.yukonga.miuix.kmp.basic.SnackbarHostState

object Global {
    var workpaperAppScope: CoroutineScope? = null
    var snackbarHostState: SnackbarHostState? = null
}