package jarvay.workpaper.compose.style

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import jarvay.workpaper.compose.Route
import jarvay.workpaper.viewModel.StyleFormViewModel

@Composable
fun StyleUpdateScreen(
    styleId: Long,
    onNavigate: (Route) -> Unit,
    viewModel: StyleFormViewModel = hiltViewModel { factory: StyleFormViewModel.Factory ->
        factory.create(styleId)
    },
) {
    StyleForm(
        onNavigate = onNavigate,
        values = viewModel.style
    ) { style ->
        viewModel.update(style.copy())
        onNavigate(Route.Home)
    }
}