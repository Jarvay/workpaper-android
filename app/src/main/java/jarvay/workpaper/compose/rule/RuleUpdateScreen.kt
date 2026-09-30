package jarvay.workpaper.compose.rule

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import jarvay.workpaper.R
import jarvay.workpaper.compose.Route
import jarvay.workpaper.compose.components.LocalSimpleSnackbar
import jarvay.workpaper.viewModel.RuleFormViewModel

@Composable
fun RuleUpdateScreen(
    ruleId: Long,
    onNavigate: (Route) -> Unit,
    viewModel: RuleFormViewModel = hiltViewModel { factory: RuleFormViewModel.Factory ->
        factory.create(ruleId)
    }
) {
    val simpleSnackbar = LocalSimpleSnackbar.current

    val rule = viewModel.ruleWithRelation?.rule

    RuleForm(
        onNavigate = onNavigate,
        values = viewModel.ruleWithRelation,
        viewModel = viewModel,
    ) { r ->
        rule?.let {
            val exists = viewModel.exists(r.startHour, r.startMinute, r.days, rule.ruleId)
            if (exists) {
                simpleSnackbar.show(R.string.rule_conflicts_tips)
                return@RuleForm
            }
            viewModel.update(r.copy(ruleId = rule.ruleId))
            onNavigate(Route.Home)
        }
    }
}