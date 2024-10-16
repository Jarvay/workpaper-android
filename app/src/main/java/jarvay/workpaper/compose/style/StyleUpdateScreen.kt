package jarvay.workpaper.compose.style

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import jarvay.workpaper.viewModel.StyleFormViewModel

@Composable
fun StyleUpdateScreen(
    navController: NavController,
    viewModel: StyleFormViewModel = hiltViewModel(),
) {
    StyleForm(
        navController = navController,
        values = viewModel.style
    ) { style ->
        viewModel.update(style.copy())
        navController.navigateUp()
    }
}