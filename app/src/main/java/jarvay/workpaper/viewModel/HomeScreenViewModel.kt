package jarvay.workpaper.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jarvay.workpaper.BuildConfig
import jarvay.workpaper.R
import jarvay.workpaper.Workpaper
import jarvay.workpaper.data.preferences.RunningPreferencesKeys
import jarvay.workpaper.data.preferences.RunningPreferencesRepository
import jarvay.workpaper.data.preferences.SettingsPreferencesRepository
import jarvay.workpaper.request.RetrofitClient
import jarvay.workpaper.request.response.Version
import jarvay.workpaper.others.STATE_IN_STATED
import jarvay.workpaper.others.showToast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    val runningPreferencesRepository: RunningPreferencesRepository,
    val settingsPreferencesRepository: SettingsPreferencesRepository,
    private val workpaper: Workpaper,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    @Inject
    lateinit var retrofitClient: RetrofitClient

    val runningPreferences = runningPreferencesRepository.runningPreferencesFlow.stateIn(
        viewModelScope,
        STATE_IN_STATED,
        null
    )

    val latestVersion = MutableLiveData<Version>()

    val upgradeDialogShow = MutableLiveData(false)

    val checkingUpdate = MutableLiveData(false)

    init {
        Log.d(javaClass.simpleName, "init")
        viewModelScope.launch {
            val settings = settingsPreferencesRepository.settingsPreferencesFlow.first()
            if (settings.autoCheckUpdate) {
                checkUpdate(silent = true)
            }
        }
    }

    fun start() {
        viewModelScope.launch {
            runningPreferencesRepository.apply {
                update(RunningPreferencesKeys.RUNNING, true)
            }
        }
        workpaper.start(viewModelScope)
    }

    fun stop() {
        workpaper.stop(
            viewModelScope,
            runningPreferencesRepository
        )
        viewModelScope.launch {
            runningPreferencesRepository.apply {
                update(RunningPreferencesKeys.RUNNING, false)
            }
        }
    }

    fun checkUpdate(silent: Boolean = false) {
        checkingUpdate.value = true
        viewModelScope.launch {
            try {
                val result = retrofitClient.updateService.data()
                Log.d(javaClass.simpleName, listOf("checkUpdate", result).joinToString(", "))

                val latest = result.latestVersion

                Log.d(
                    "BuildConfig.VERSION_CODE < latestVersion.versionCode",
                    (BuildConfig.VERSION_CODE < latest.versionCode).toString()
                )

                if (BuildConfig.VERSION_CODE < latest.versionCode) {
                    latestVersion.value = latest
                    upgradeDialogShow.value = true
                } else if (!silent) {
                    showToast(context, R.string.tips_no_new_version)
                }
                checkingUpdate.value = false
            } catch (e: Exception) {
                showToast(context, R.string.tips_check_update_failed)
                Log.d("checkUpdate failed", e.toString())
            }
        }
    }
}