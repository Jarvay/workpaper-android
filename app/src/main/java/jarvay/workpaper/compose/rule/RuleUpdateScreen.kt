package jarvay.workpaper.compose.rule

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.viewModel.RuleFormViewModel

@Composable
fun RuleUpdateScreen(navController: NavController, viewModel: RuleFormViewModel = hiltViewModel()) {
    val simpleSnackbar = LocalSimpleSnackbar.current

    val rule = viewModel.ruleAlbums?.rule

    RuleForm(
        navController = navController,
        values = viewModel.ruleAlbums
    ) { r ->
        rule?.let {
            val exists = viewModel.exists(r.startHour, r.startMinute, r.days, rule.ruleId)
            if (exists) {
                simpleSnackbar.show(R.string.rule_conflicts_tips)
                return@RuleForm
            }
            viewModel.update(r.copy(ruleId = rule.ruleId))
            navController.navigateUp()
        }
    }
}