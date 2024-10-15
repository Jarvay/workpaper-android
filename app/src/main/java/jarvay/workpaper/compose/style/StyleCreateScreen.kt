package jarvay.workpaper.compose.style

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import jarvay.workpaper.viewModel.StyleFormViewModel

@Composable
fun StyleCreateScreen(
    navController: NavController,
    viewModel: StyleFormViewModel = hiltViewModel(),
) {
    StyleForm(
        navController = navController,
    ) { style ->
        viewModel.insert(style.copy())
        navController.navigateUp()
    }
}