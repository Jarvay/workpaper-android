package jarvay.workpaper.compose.style

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import jarvay.workpaper.compose.Route
import jarvay.workpaper.viewModel.StyleFormViewModel

@Composable
fun StyleCreateScreen(
    onNavigate: (Route) -> Unit,
    viewModel: StyleFormViewModel = hiltViewModel { factory: StyleFormViewModel.Factory ->
        factory.create(null)
    },
) {
    StyleForm(
        onNavigate = onNavigate,
    ) { style ->
        viewModel.insert(style.copy())
        onNavigate(Route.Home)
    }
}