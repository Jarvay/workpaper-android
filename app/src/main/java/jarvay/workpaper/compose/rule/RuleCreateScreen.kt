package jarvay.workpaper.compose.rule

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import jarvay.workpaper.others.FormMode
import jarvay.workpaper.viewModel.RuleFormViewModel

@Composable
fun RuleCreateScreen(navController: NavController, viewModel: RuleFormViewModel = hiltViewModel()) {
    val albums by viewModel.allAlbums.observeAsState(initial = emptyList())

    RuleForm(
        navController = navController,
        albums = albums
    ) { rule ->
        viewModel.insert(rule.copy())
        navController.navigateUp()
    }
}