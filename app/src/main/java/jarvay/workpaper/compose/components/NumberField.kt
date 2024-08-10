package jarvay.workpaper.compose.components

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun NumberField(
    value: Int,
    onValueChange: (Int) -> Unit,
    step: Int = 1,
    min: Int? = null,
    max: Int? = null,
    label: @Composable (() -> Unit)? = null
) {
    var lastValidValue by remember {
        mutableStateOf(value.toString())
    }
    var content by remember {
        mutableStateOf(value.toString())
    }

    val onChange: (Int) -> Unit = { newValue ->
        Log.d("onChange", newValue.toString())
        var result = newValue

        min?.let {
            if (newValue < min) {
                result = min
            }
        }
        max?.let {
            if (newValue > max) {
                result = max
            }
        }
        content = result.toString()
        onValueChange(result)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally
        ),
    ) {
        val buttonModifier: Modifier = Modifier.weight(0.2F)

        CustomIconButton(onClick = { onChange(value - step) }, modifier = buttonModifier) {
            Icon(imageVector = Icons.Default.Remove, contentDescription = "")
        }

        OutlinedTextField(
            modifier = Modifier
                .weight(0.6F, true)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        try {
                            onChange(content.toInt())
                            lastValidValue = content
                        } catch (_: Exception) {
                            content = lastValidValue
                        }
                    }
                }
                .focusable(true),
            label = label,
            value = content,
            onValueChange = {
                content = it
                if (it.isNotBlank() && it.isNotEmpty()) {
                    try {
                        onChange(content.toInt())
                        lastValidValue = content
                    } catch (_: Exception) {
                        content = lastValidValue
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(textAlign = TextAlign.Center),
        )

        CustomIconButton(onClick = { onChange(value + step) }, modifier = buttonModifier) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "")
        }
    }
}