package com.example.oylfrutossecos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.ProfileDraft
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.ui.viewmodel.ProfileState

@Composable
fun EditProfileScreen(state: ProfileState, onChange: (ProfileDraft) -> Unit, onSave: () -> Unit, onBack: () -> Unit) {
    BackHandler(enabled = state.busy) { }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp).testTag("edit-profile"),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack, enabled = !state.busy) { Text("← Volver a Cuenta") }
        ReferenceTitle("Editar perfil")
        Text("Actualiza tus datos personales. El alias, correo, rol y contraseña se mantienen.", style = MaterialTheme.typography.bodySmall)
        AuthField("Nombre", state.draft.name, { onChange(state.draft.copy(name = it)) }, Icons.Default.Person,
            error = state.errors["name"], enabled = !state.busy)
        AuthField("Teléfono", state.draft.phone, { onChange(state.draft.copy(phone = it)) }, Icons.Default.Phone,
            error = state.errors["phone"], enabled = !state.busy, keyboardType = KeyboardType.Phone)
        AuthField("Dirección", state.draft.address, { onChange(state.draft.copy(address = it)) }, Icons.Default.LocationOn,
            error = state.errors["address"], enabled = !state.busy)
        state.error?.let { InfoBanner("No se pudo guardar", it, error = true) }
        Button(onClick = onSave, enabled = !state.busy && !state.saved,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(if (state.busy) "Guardando perfil…" else "Guardar cambios")
        }
    }
}
