package jarvay.workpaper.others

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

fun requestAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val uri = Uri.parse("package:" + context.packageName)
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent)
    }
}

fun requestNotificationPermission(context: Context) {
    val intent = Intent().apply {
        action = "android.settings.APP_NOTIFICATION_SETTINGS"
        putExtra("app_package", context.packageName)
        putExtra("app_uid", context.applicationInfo.uid)
        putExtra("android.provider.extra.APP_PACKAGE", context.packageName);
    }
    context.startActivity(intent)
}

fun Context.grantUriPermissionToHomeActivity(uri: Uri, flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION) {
    grantUriPermission(packageManager.homeActivityPackages(), uri, flags)
}

fun PackageManager.homeActivityPackages(): List<String> {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_HOME)
    return queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        .map { it.activityInfo.packageName }
}

fun Context.grantUriPermission(packages: List<String>, uri: Uri, flags: Int) {
    for (name in packages) {
        grantUriPermission(name, uri, flags)
    }
}