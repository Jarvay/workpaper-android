package jarvay.workpaper.compose.rule

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import jarvay.workpaper.viewModel.RuleFormViewModel

@Composable
fun RuleUpdateScreen(navController: NavController, viewModel: RuleFormViewModel = hiltViewModel()) {
    val albums by viewModel.allAlbums.observeAsState(initial = emptyList())

    val rule = viewModel.rule?.rule

    RuleForm(
        navController = navController,
        albums = albums,
        values = viewModel.rule,
    ) { r ->
        rule?.let {
            viewModel.update(r.copy(ruleId = rule.ruleId))
            navController.navigateUp()
        }
    }
}