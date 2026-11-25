package com.example.oylfrutossecos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AdminAccessPanel(configured: Boolean, enabled: Boolean, onConnect: (String) -> Unit, onClear: () -> Unit,
    validating: Boolean = false, error: String? = null) {
    // Nunca rememberSaveable: Android no debe serializar esta clave.
    var value by remember { mutableStateOf("") }
    var replacing by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(configured) { if (configured) { replacing = false; value = "" } }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (configured) {
                Text("Acceso administrativo conectado", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall)
                Text("Puedes gestionar los pedidos y el inventario durante esta sesión.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { replacing = !replacing; value = "" }, enabled = enabled && !validating) {
                    Text(if (replacing) "Cancelar cambio" else "Cambiar clave")
                }
            }
            if (!configured || replacing) {
                Text("Introduce la clave de acceso proporcionada por la administración (mínimo 16 caracteres).",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value, { value = it }, label = { Text("Clave administrativa") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    enabled = enabled && !validating, modifier = Modifier.fillMaxWidth())
                Button(onClick = { keyboard?.hide(); onConnect(value); value = "" }, enabled = enabled && !validating && value.length >= 16,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (validating) "Validando acceso…" else "Conectar administración")
                }
            }
            error?.let { Text(storeMessage(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (configured) TextButton(onClick = { value = ""; onClear() }, enabled = enabled && !validating) {
                Text("Desconectar acceso")
            }
            Text("La clave solo permanece en memoria. Se elimina al cerrar sesión o reiniciar la aplicación.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdminSettingsScreen(configured: Boolean, enabled: Boolean, validating: Boolean, error: String?,
    onConnect: (String) -> Unit, onClear: () -> Unit, onBack: () -> Unit) {
    BackHandler(enabled = validating) { }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(12.dp)
        .testTag("admin-settings"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReferenceTitle("Configuración administrativa", if (validating) null else onBack)
        AdminAccessPanel(configured, enabled, onConnect, onClear, validating, error)
    }
}

@Composable
fun AdminAccessRequired(title: String, onBack: () -> Unit, onConfigure: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReferenceTitle(title, onBack)
        InfoBanner("Acceso administrativo requerido", "Para continuar, valida tu acceso de administración. Puedes configurarlo aquí sin cerrar sesión.")
        Button(onClick = onConfigure) { Text("Configurar acceso") }
    }
}
