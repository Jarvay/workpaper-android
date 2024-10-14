package jarvay.workpaper.compose.rule

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import jarvay.workpaper.R
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.viewModel.RuleFormViewModel

@Composable
fun RuleCreateScreen(navController: NavController, viewModel: RuleFormViewModel = hiltViewModel()) {
    val simpleSnackbar = LocalSimpleSnackbar.current

    RuleForm(
        navController = navController,
        viewModel = viewModel,
    ) { rule ->
        val exists = viewModel.exists(rule.startHour, rule.startMinute, rule.days)
        if (exists) {
            simpleSnackbar.show(R.string.rule_conflicts_tips)
            return@RuleForm
        }
        viewModel.insert(rule.copy())
        navController.navigateUp()
    }
}