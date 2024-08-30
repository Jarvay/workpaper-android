package jarvay.workpaper.compose.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import jarvay.workpaper.viewModel.MainActivityViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LocalMainActivityModel = staticCompositionLocalOf<MainActivityViewModel> { error("") }

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("") }

class SimpleSnackbar(
    val context: Context,
    private val scope: CoroutineScope,
    private val hostState: SnackbarHostState
) {
    fun show(message: String) {
        scope.launch {
            hostState.showSnackbar(message)
        }
    }

    fun show(@StringRes strId: Int) {
        show(context.getString(strId))
    }
}

val LocalSimpleSnackbar = staticCompositionLocalOf<SimpleSnackbar> { error("") }