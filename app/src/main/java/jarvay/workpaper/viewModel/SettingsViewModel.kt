package jarvay.workpaper.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.data.Settings
import jarvay.workpaper.others.SharePreferenceKey
import jarvay.workpaper.others.defaultSharedPreferences
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    private val gson = Gson()

    fun saveSettings(settings: Settings, context: Context) {
        val sp = defaultSharedPreferences(context)
        viewModelScope.launch {
            sp.edit().apply {
                putString(SharePreferenceKey.SETTINGS_KEY, gson.toJson(settings))
            }.apply()
        }
    }
}