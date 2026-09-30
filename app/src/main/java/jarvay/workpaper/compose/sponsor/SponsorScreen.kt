package jarvay.workpaper.compose.sponsor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import coil3.compose.AsyncImage
import com.blankj.utilcode.util.LogUtils
import jarvay.workpaper.compose.Route
import jarvay.workpaper.request.REPO_MIRRORS_MAP
import jarvay.workpaper.request.RepoHost
import jarvay.workpaper.viewModel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back


@Composable
fun SponsorScreen(onNavigate: (Route) -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
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
            SmallTopAppBar(
                title = "",
                navigationIcon = {
                    IconButton(onClick = { onNavigate(Route.Home) }) {
                        Icon(MiuixIcons.Back, "")
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