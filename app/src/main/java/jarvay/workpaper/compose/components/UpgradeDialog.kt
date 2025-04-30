package jarvay.workpaper.compose.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jarvay.workpaper.MainActivity
import jarvay.workpaper.R
import jarvay.workpaper.others.download
import jarvay.workpaper.viewModel.MainActivityViewModel

@Composable
fun UpgradeDialog(
    viewModal: MainActivityViewModel, simpleSnackbar: SimpleSnackbar
) {
    val upgradeDialogShow by viewModal.upgradeDialogShow.observeAsState(initial = false)
    val latestVersion by viewModal.latestVersion.observeAsState()
    val updatingLogs by viewModal.updatingLogs.observeAsState(initial = emptyList())

    val context = LocalContext.current

    SimpleDialog(show = upgradeDialogShow, title = {
        Text(text = stringResource(id = R.string.tips_new_app_version))
    }, content = {
        LazyColumn(
            modifier = Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = updatingLogs, key = { it.versionName }) {
                Row {
                    Text(text = it.versionName)
                }
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(text = it.desc)
                }
            }
        }
    }, onDismissRequest = {
        viewModal.upgradeDialogShow.value = false
    }) {
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