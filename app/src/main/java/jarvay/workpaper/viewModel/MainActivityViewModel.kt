package jarvay.workpaper.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.LogUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import jarvay.workpaper.BuildConfig
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.request.RetrofitClient
import jarvay.workpaper.request.response.Version
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settingsPreferencesRepository: SettingsPreferencesRepository,
    val runningPreferencesRepository: RunningPreferencesRepository
) : ViewModel() {
    @Inject
    lateinit var retrofitClient: RetrofitClient

    val settings =
        settingsPreferencesRepository.settingsPreferencesFlow.asLiveData()

    val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.asLiveData()

    val latestVersion = MutableLiveData<Version>()

    val upgradeDialogShow = MutableLiveData(false)

    val checkingUpdate = MutableLiveData(false)

    init {
        viewModelScope.launch {
            val settings = settingsPreferencesRepository.settingsPreferencesFlow.first()
            if (settings.autoCheckUpdate) {
                checkUpdate()
            }
        }
    }

    fun checkUpdate(onError: () -> Unit = {}, onResult: (Boolean) -> Unit = {}) {
        checkingUpdate.value = true
        viewModelScope.launch {
            try {
                val result = retrofitClient.updateService.data()
                val latest = result.latestVersion

                var hasNewVersion = false
                if (BuildConfig.VERSION_CODE < latest.versionCode) {
                    latestVersion.value = latest
                    upgradeDialogShow.value = true
                    hasNewVersion = true
                }
                checkingUpdate.value = false
                onResult(hasNewVersion)
            } catch (e: Exception) {
                checkingUpdate.value = false
                LogUtils.i(javaClass.simpleName, "checkUpdate failed", e.toString())
                onError()
            }
        }
    }
}