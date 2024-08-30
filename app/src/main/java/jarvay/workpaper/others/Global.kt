package jarvay.workpaper.others

import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope

object Global {
    var workpaperAppScope: CoroutineScope? = null
    var snackbarHostState: SnackbarHostState? = null
}