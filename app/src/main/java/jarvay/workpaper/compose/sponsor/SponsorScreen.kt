package jarvay.workpaper.compose.sponsor

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.blankj.utilcode.util.LogUtils
import jarvay.workpaper.request.REPO_MIRRORS_MAP
import jarvay.workpaper.request.RepoHost
import jarvay.workpaper.viewModel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SponsorScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    var loading by remember {
        mutableStateOf(true)
    }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val mirror = (REPO_MIRRORS_MAP[settings.repoMirror]
        ?: REPO_MIRRORS_MAP[RepoHost.GH_FAST.value]).toString()

    val wechatImgUrl = mirror + "wechat.png"
    LogUtils.i("SponsorScreen", wechatImgUrl)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth(),
        ) {
            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AsyncImage(
                model = wechatImgUrl,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .width(240.dp),
                onSuccess = {
                    loading = false
                }
            )
        }
    }
}