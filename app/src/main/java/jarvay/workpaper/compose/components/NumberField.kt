package jarvay.workpaper.compose.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val onChange: (Int) -> Unit = {
        var newValue = it
        min?.let {
            if (it < min) {
                newValue = min
            }
        }
        max?.let {
            if (it > max) {
                newValue = max
            }
        }
        onValueChange(newValue)
    }



    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally
        ),
    ) {
        val buttonModifier: Modifier = Modifier.weight(0.2F)

        IconButton(onClick = { onChange(value - step) }, modifier = buttonModifier) {
            Icon(imageVector = Icons.Default.Remove, contentDescription = "")
        }

        TextField(
            modifier = Modifier.weight(0.6F, true),
            label = label,
            value = value.toString(),
            onValueChange = {
                try {
                    onChange(it.toInt())
                } catch (e: Exception) {
                    Log.d(javaClass.simpleName, it)
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(textAlign = TextAlign.Center)
        )

        IconButton(onClick = { onChange(value + step) }, modifier = buttonModifier) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "")
        }
    }
}