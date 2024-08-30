package jarvay.workpaper.request.response

import jarvay.workpaper.BuildConfig

data class Version(
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val versionDesc: String? = "",
    val apkUrl: String = "",
)

data class Release(
    val latestVersion: Version
)