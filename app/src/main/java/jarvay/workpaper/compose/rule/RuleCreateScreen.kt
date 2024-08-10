package jarvay.workpaper.compose.rule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.others.showToast
import jarvay.workpaper.viewModel.RuleFormViewModel

@Composable
fun RuleCreateScreen(navController: NavController, viewModel: RuleFormViewModel = hiltViewModel()) {
    val albums by viewModel.allAlbums.observeAsState(initial = emptyList())
    val context = LocalContext.current

    RuleForm(
        navController = navController,
    ) { rule ->
        val exists = viewModel.exists(rule.startHour, rule.startMinute, rule.days)
        if (exists) {
            showToast(context, R.string.rule_conflicts_tips)
            return@RuleForm
        }
        viewModel.insert(rule.copy())
        navController.navigateUp()
    }
}