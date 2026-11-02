package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.UserRole
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.ui.viewmodel.LoginState

@Composable
fun LoginScreen(
    state: LoginState,
    onAliasChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit
) {
    AuthLayout("Iniciar sesión", "Ingresa con tu alias y contraseña.", branded = true) {
        OylBrand(Modifier.height(145.dp), compact = true, decorated = false)
        Text("Tipo de usuario", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UserRole.values().forEach { role ->
                Surface(shape = MaterialTheme.shapes.large,
                    color = if (state.role == role) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .selectable(selected = state.role == role, enabled = !state.busy,
                            role = Role.RadioButton, onClick = { onRoleChange(role) })) {
                    Box(contentAlignment = Alignment.Center) { Text(role.label, style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
        AuthField("Alias", state.alias, onAliasChange, Icons.Default.Person,
            error = state.errors["alias"], enabled = !state.busy)
        AuthField("Contraseña", state.password, onPasswordChange, Icons.Default.Lock,
            error = state.errors["password"], enabled = !state.busy, password = true)
        AuthMessage(state.errorMessage)
        AuthMessage(state.successMessage, isError = false)
        AuthButton("Ingresar", state.busy, onLoginClick)
        TextButton(onClick = onRegisterClick, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth()) {
            Text("Crear cuenta de cliente")
        }
        OutlinedButton(onClick = onBackClick, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}
