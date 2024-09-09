package jarvay.workpaper.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.Workpaper
import jarvay.workpaper.others.STATE_IN_STATED
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ActionWidgetViewModel @Inject constructor(): ViewModel() {
    @Inject
    lateinit var workpaper: Workpaper

    val nextWallpaper = workpaper.nextWallpaper.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        null
    )
}