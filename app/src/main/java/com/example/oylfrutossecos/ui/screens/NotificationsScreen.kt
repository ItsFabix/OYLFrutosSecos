package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.AppNotice
import com.example.oylfrutossecos.ui.viewmodel.NotificationUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    state: NotificationUiState, onBack: () -> Unit, onRefresh: () -> Unit,
    onMarkRead: (Long) -> Unit, onMarkAllRead: () -> Unit,
    onOpen: (AppNotice) -> Unit, onTest: () -> Unit,
    permissionGranted: Boolean, onRequestPermission: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            TextButton(onClick = onBack) { Text("← Volver") }
            Text("Notificaciones", style = MaterialTheme.typography.headlineMedium)
            Text("Avisos guardados en este dispositivo; no se reciben notificaciones del servidor.",
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text("${state.unreadCount} sin leer", color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefresh) { Text("Actualizar") }
                TextButton(onClick = onMarkAllRead,
                    enabled = state.unreadCount > 0 && !state.busy) { Text("Leer todas") }
            }
        }
        item {
            OutlinedCard {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Avisos en el dispositivo", style = MaterialTheme.typography.titleMedium)
                    if (!permissionGranted) {
                        Text("Activa el permiso para mostrar avisos fuera de la app.",
                            style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onRequestPermission) { Text("Permitir notificaciones") }
                    }
                    Button(onClick = onTest, enabled = !state.busy) {
                        Text("Crear aviso local")
                    }
                }
            }
        }
        if (state.message != null) item {
            Text(state.message, color = MaterialTheme.colorScheme.primary)
        }
        if (state.loading) item { CircularProgressIndicator() }
        if (state.error != null) item {
            Text(state.error, color = MaterialTheme.colorScheme.error)
        }
        if (!state.loading && state.items.isEmpty()) item {
            Text("Todavía no tienes avisos. Puedes enviar uno con el botón de arriba.")
        }
        items(state.items, key = { it.id }) { notice ->
            OutlinedCard(modifier = Modifier.fillMaxWidth().clickable {
                onMarkRead(notice.id)
                onOpen(notice)
            }) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (notice.isRead) notice.title else "● ${notice.title}",
                        style = MaterialTheme.typography.titleMedium)
                    Text(notice.message)
                    Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("es", "CL"))
                        .format(Date(notice.createdAt)), style = MaterialTheme.typography.bodySmall)
                    Text(if (notice.isRead) "Leída" else "Sin leer · Toca para abrir",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
