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
    AuthLayout("Iniciar sesión", "Ingresa con tu alias y contraseña.") {
        Text("Tipo de usuario", style = MaterialTheme.typography.titleSmall)
        Column(Modifier.fillMaxWidth().selectableGroup()) {
            UserRole.values().forEach { role ->
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .selectable(selected = state.role == role, enabled = !state.busy,
                            role = Role.RadioButton, onClick = { onRoleChange(role) })
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = state.role == role, onClick = null, enabled = !state.busy)
                    Spacer(Modifier.width(12.dp))
                    Text(role.label)
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
