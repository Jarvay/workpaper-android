package jarvay.workpaper.others

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import jarvay.workpaper.receiver.DeviceManagerReceiver


fun requestAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val uri = Uri.parse("package:" + context.packageName)
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

fun requestNotificationPermission(context: Context) {
    val intent = Intent().apply {
        action = "android.settings.APP_NOTIFICATION_SETTINGS"
        putExtra("app_package", context.packageName)
        putExtra("app_uid", context.applicationInfo.uid)
        putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
    }
    context.startActivity(intent)
}

fun deviceAdminIntent(context: Context): Intent {
    return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(
            DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(
                context,
                DeviceManagerReceiver::class.java
            )
        )
    }
}