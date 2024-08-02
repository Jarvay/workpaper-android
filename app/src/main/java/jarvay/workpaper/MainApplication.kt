/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jarvay.workpaper

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import jarvay.workpaper.others.SharePreferenceKey
import jarvay.workpaper.others.defaultSharedPreferences
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {
  @Inject
  lateinit var workerFactory: HiltWorkerFactory

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder()
      .setWorkerFactory(workerFactory)
      .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
      .build()

  override fun onCreate() {
    super.onCreate()
    val isRunning = defaultSharedPreferences(this).getBoolean(SharePreferenceKey.IS_RUNNING_KEY, false)
    if (isRunning) {
      start(this)
    }
  }
}
