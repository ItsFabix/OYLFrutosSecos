package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.data.model.UserRole
import com.example.oylfrutossecos.ui.components.*

@Composable
fun AccountScreen(user: User, busy: Boolean, error: String?,
    onLogoutClick: () -> Unit, onOrdersClick: () -> Unit, onMapClick: () -> Unit,
    onNotificationsClick: () -> Unit, unreadNotices: Int,
    onRestClick: () -> Unit = {}) {
    AuthLayout(
        title = "Hola, ${user.name}",
        subtitle = if (user.role == UserRole.ADMIN) "Cuenta de administrador"
            else "Mi cuenta de cliente"
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Datos de la cuenta", style = MaterialTheme.typography.titleMedium)
                Text("Alias: ${user.alias}")
                Text("Correo: ${user.email}")
                Text("Teléfono: ${user.phone}")
                Text("Dirección: ${user.address}")
            }
        }
        OutlinedButton(onClick = onOrdersClick, enabled = !busy,
            modifier = Modifier.fillMaxWidth()) {
            Text(if (user.role == UserRole.ADMIN) "Gestionar pedidos" else "Mis pedidos")
        }
        OutlinedButton(onClick = onNotificationsClick, enabled = !busy,
            modifier = Modifier.fillMaxWidth()) {
            Text("Notificaciones" + if (unreadNotices > 0) " ($unreadNotices)" else "")
        }
        OutlinedButton(onClick = onMapClick, enabled = !busy,
            modifier = Modifier.fillMaxWidth()) {
            Text("Puntos de retiro")
        }
        OutlinedButton(onClick = onRestClick, enabled = !busy,
            modifier = Modifier.fillMaxWidth()) {
            Text("Conexión con el servidor")
        }
        AuthMessage(error)
        AuthButton("Cerrar sesión", busy, onLogoutClick)
    }
}
