package jarvay.workpaper.compose.rule

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import jarvay.workpaper.R
import jarvay.workpaper.compose.Route
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.viewModel.RuleFormViewModel

@Composable
fun RuleCreateScreen(
    onNavigate: (Route) -> Unit,
    viewModel: RuleFormViewModel = hiltViewModel { factory: RuleFormViewModel.Factory ->
        factory.create(null)
    }
) {
    val simpleSnackbar = LocalSimpleSnackbar.current

    RuleForm(
        onNavigate = onNavigate,
        viewModel = viewModel,
    ) { rule ->
        val exists = viewModel.exists(rule.startHour, rule.startMinute, rule.days)
        if (exists) {
            simpleSnackbar.show(R.string.rule_conflicts_tips)
            return@RuleForm
        }
        viewModel.insert(rule.copy())
        onNavigate(Route.Home)
    }
}