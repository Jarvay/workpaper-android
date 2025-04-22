package jarvay.workpaper.compose.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource


@Composable
fun SettingsItem(
    @StringRes labelId: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SettingsItem(label = stringResource(id = labelId), modifier = modifier, content = content)
}

@Composable
fun SettingsItem(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text = label)

        content()
    }
}