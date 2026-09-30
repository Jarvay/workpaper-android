package jarvay.workpaper.compose.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CustomIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick, modifier = modifier, enabled = enabled, content = content
    )
}

@Composable
fun CustomIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imageVector: ImageVector,
    contentDescription: String? = null
) {
    IconButton(
        onClick = onClick, modifier = modifier, enabled = enabled, content = {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.disabledOnPrimaryButton
            )
        })
}