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
fun RuleUpdateScreen(navController: NavController, viewModel: RuleFormViewModel = hiltViewModel()) {
    val albums by viewModel.allAlbums.observeAsState(initial = emptyList())
    val context = LocalContext.current

    val rule = viewModel.rule?.rule

    RuleForm(
        navController = navController,
        albums = albums,
        values = viewModel.rule,
    ) { r ->
        rule?.let {
            val exists = viewModel.exists(r.startHour, r.startMinute, r.days, rule.ruleId)
            if (exists) {
                showToast(context, R.string.rule_conflicts_tips)
                return@RuleForm
            }
            viewModel.update(r.copy(ruleId = rule.ruleId))
            navController.navigateUp()
        }
    }
}