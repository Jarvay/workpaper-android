package jarvay.workpaper.request.response

data class UpdatingLogItem(
    val versionName: String,
    val versionCode: Int,
    val desc: String,
    val apkUrl: String,
)