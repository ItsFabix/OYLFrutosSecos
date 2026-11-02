package com.example.oylfrutossecos.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun AdminFormField(label: String, value: String, onChange: (String) -> Unit,
    icon: ImageVector, error: String?, help: String, enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text, readOnly: Boolean = false,
    minLines: Int = 1) {
    OutlinedTextField(
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, enabled = enabled, readOnly = readOnly,
        isError = error != null, supportingText = { Text(error ?: help) },
        leadingIcon = { Icon(icon, contentDescription = label) },
        trailingIcon = if (error != null) ({
            Icon(Icons.Default.Error, contentDescription = "Error en $label")
        }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = minLines == 1, minLines = minLines)
}
