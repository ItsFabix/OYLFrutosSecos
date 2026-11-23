package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.data.model.UserRole
import com.example.oylfrutossecos.ui.components.*

@Composable
fun AccountScreen(user: User, busy: Boolean, error: String?,
    onLogoutClick: () -> Unit, onOrdersClick: () -> Unit, onMapClick: () -> Unit,
    onNotificationsClick: () -> Unit, unreadNotices: Int,
    onEditProfile: () -> Unit = {}, notice: String? = null, onAdminSettings: () -> Unit = {}) {
    var showData by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().testTag("account-list"), contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ReferenceTitle("Mi perfil") }
        notice?.let { item { InfoBanner("Perfil actualizado", it) } }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.Person, null, Modifier.padding(12.dp).size(34.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text(user.name, style = MaterialTheme.typography.titleMedium)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("@${user.alias} · " + if (user.role == UserRole.ADMIN) "Administrador" else "Cliente",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Column {
                    AccountAction("Editar perfil", Icons.Default.Edit, !busy, onEditProfile)
                    HorizontalDivider()
                    AccountAction(if (user.role == UserRole.ADMIN) "Gestionar pedidos" else "Mis pedidos",
                        Icons.AutoMirrored.Filled.ReceiptLong, !busy, onOrdersClick)
                    HorizontalDivider()
                    AccountAction("Notificaciones" + if (unreadNotices > 0) " ($unreadNotices)" else "",
                        Icons.Default.Notifications, !busy, onNotificationsClick)
                    HorizontalDivider()
                    AccountAction("Puntos de retiro", Icons.Default.LocationOn, !busy, onMapClick)
                    HorizontalDivider()
                    if (user.role == UserRole.ADMIN) {
                        AccountAction("Configuración administrativa", Icons.Default.Settings, !busy, onAdminSettings)
                        HorizontalDivider()
                    }
                    AccountAction(if (busy) "Cerrando sesión…" else "Cerrar sesión", Icons.Default.Logout, !busy, onLogoutClick)
                }
            }
        }
        item {
            AccountAction("Datos de la cuenta", Icons.Default.Person, !busy) { showData = !showData }
            if (showData) Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AccountDetail("Alias", user.alias)
                    AccountDetail("Correo", user.email)
                    AccountDetail("Teléfono", user.phone)
                    AccountDetail("Dirección", user.address)
                }
            }
        }
        item { AuthMessage(error) }
    }

}

@Composable
private fun AccountAction(label: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccountDetail(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
