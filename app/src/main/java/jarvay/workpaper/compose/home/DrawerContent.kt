package jarvay.workpaper.compose.home

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jarvay.workpaper.BuildConfig
import jarvay.workpaper.R
import jarvay.workpaper.compose.Route
import jarvay.workpaper.compose.components.LocalMainActivityModel
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DrawerContent(
    onNavigate: (Route) -> Unit,
    drawerState: DrawerState,
) {
    val simpleSnackbar = LocalSimpleSnackbar.current
    val mainActivityViewModel = LocalMainActivityModel.current

    val scope = rememberCoroutineScope()

    val checkingUpdate by mainActivityViewModel.checkingUpdate.observeAsState(initial = false)

    Log.d("BuildConfig.VERSION_NAME", BuildConfig.VERSION_NAME)

    Column(
        modifier = Modifier.fillMaxSize(0.75f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MiuixTheme.textStyles.title1,
                color = MiuixTheme.colorScheme.primary
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = BuildConfig.VERSION_NAME,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.secondary
            )
        }
        HorizontalDivider()
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            ArrowPreference(
                title = stringResource(id = R.string.drawer_menu_check_update),
                enabled = !checkingUpdate,
                startAction = {
                    if (checkingUpdate) {
                        Row {
                            InfiniteProgressIndicator()
                            Spacer(modifier = Modifier.padding(end = 16.dp))
                        }

                    } else {
                        Icon(
                            imageVector = MiuixIcons.Update,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                onClick = {
                    if (checkingUpdate) return@ArrowPreference
                    mainActivityViewModel.checkUpdate(
                        onError = {
                            simpleSnackbar.show(R.string.tips_check_update_failed)
                        }) {
                        if (!it) {
                            simpleSnackbar.show(R.string.tips_no_new_version)
                        }
                    }
                })

            ArrowPreference(
                title = stringResource(id = R.string.drawer_menu_sponsor),
                startAction = {
                    Icon(
                        imageVector = Icons.Default.Money,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                onClick = {
                    onNavigate(Route.Sponsor)
                    scope.launch {
                        drawerState.close()
                    }
                })
        }
    }
}