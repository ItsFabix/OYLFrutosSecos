package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.oylfrutossecos.data.model.Registration
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.ui.viewmodel.RegisterState

@Composable
fun RegisterScreen(
    state: RegisterState,
    onFormChange: (Registration) -> Unit,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val form = state.form
    val enabled = !state.busy && !state.completed
    AuthLayout("Crear cuenta", "Demostración académica: utiliza solamente datos ficticios.") {
        AuthField("Nombre", form.name, { onFormChange(form.copy(name = it)) },
            Icons.Default.Person, state.errors["name"], enabled)
        AuthField("Alias", form.alias, { onFormChange(form.copy(alias = it)) },
            Icons.Default.AccountCircle, state.errors["alias"], enabled)
        AuthField("Correo de prueba", form.email, { onFormChange(form.copy(email = it)) },
            Icons.Default.Email, state.errors["email"], enabled,
            keyboardType = KeyboardType.Email)
        AuthField("Teléfono de prueba", form.phone, { onFormChange(form.copy(phone = it)) },
            Icons.Default.Phone, state.errors["phone"], enabled,
            keyboardType = KeyboardType.Phone)
        AuthField("Dirección de prueba", form.address, { onFormChange(form.copy(address = it)) },
            Icons.Default.Home, state.errors["address"], enabled)
        AuthField("Contraseña", form.password, { onFormChange(form.copy(password = it)) },
            Icons.Default.Lock, state.errors["password"], enabled, password = true)
        AuthField("Repetir contraseña", form.confirmation, { onFormChange(form.copy(confirmation = it)) },
            Icons.Default.Lock, state.errors["confirmation"], enabled, password = true)
        AuthMessage(state.errorMessage)
        AuthButton("Crear cuenta", state.busy, onRegisterClick, enabled = !state.completed)
        OutlinedButton(onClick = onBackClick, enabled = enabled,
            modifier = Modifier.fillMaxWidth()) { Text("Volver al inicio de sesión") }
    }
}
