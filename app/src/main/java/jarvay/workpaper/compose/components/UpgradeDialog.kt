package jarvay.workpaper.compose.components

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jarvay.workpaper.MainActivity
import jarvay.workpaper.R
import jarvay.workpaper.others.download
import jarvay.workpaper.viewModel.MainActivityViewModel

@Composable
fun UpgradeDialog(
    viewModal: MainActivityViewModel,
    simpleSnackbar: SimpleSnackbar
) {
    val upgradeDialogShow by viewModal.upgradeDialogShow.observeAsState(initial = false)
    val latestVersion by viewModal.latestVersion.observeAsState()

    val context = LocalContext.current

    SimpleDialog(
        show = upgradeDialogShow,
        title = {
            Text(text = stringResource(id = R.string.tips_new_app_version))
        },
        content = {
            latestVersion?.let {
                if (latestVersion!!.versionDesc?.isNotBlank() == true) {
                    Text(text = latestVersion!!.versionDesc ?: "")
                }
            }
        },
        onDismissRequest = {
            viewModal.upgradeDialogShow.value = false
        }
    ) {
        latestVersion?.let {
            simpleSnackbar.show(R.string.tips_start_downloading)
            val id = download(url = it.apkUrl, context = context)
            val intent = Intent()
            intent.setAction(MainActivity.ACTION_APK_DOWNLOAD_ID)
            intent.putExtra(MainActivity.APK_DOWNLOAD_ID_KEY, id)
            context.sendBroadcast(intent)
        }
    }
}