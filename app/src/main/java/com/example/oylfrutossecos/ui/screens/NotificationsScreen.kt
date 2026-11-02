package com.example.oylfrutossecos.ui.screens

import com.example.oylfrutossecos.ui.components.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import com.example.oylfrutossecos.data.model.NoticeKind
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
    var filter by rememberSaveable { mutableStateOf("Todas") }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val shown = state.items.filter { filter == "Todas" || (filter == "Pedidos" && it.kind in setOf(NoticeKind.ORDER_CREATED, NoticeKind.ORDER_STATUS)) || (filter == "Inventario" && it.kind == NoticeKind.STOCK_LOW) }
    LazyColumn(Modifier.fillMaxSize().testTag("notices-list"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ReferenceTitle("Notificaciones", onBack)
            Text("Avisos de tus pedidos y productos en este dispositivo.", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Todas", "Pedidos", "Inventario").forEach { label -> OylFilterChip(filter == label, { filter = label }, label = { Text(label) }) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${state.unreadCount} sin leer", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar") }
                TextButton(onClick = onMarkAllRead,
                    enabled = state.unreadCount > 0 && !state.busy) { Text("Leer todas") }
            }
        }
        if (state.message != null) item {
            Text(state.message, color = MaterialTheme.colorScheme.primary)
        }
        if (state.loading) item { CircularProgressIndicator() }
        if (state.error != null) item {
            Text(storeMessage(state.error), color = MaterialTheme.colorScheme.error)
        }
        if (!state.loading && shown.isEmpty()) item {
            EmptyState("Todo al día", "Aquí verás las novedades de tus pedidos y productos.")
        }
        items(shown, key = { it.id }) { notice ->
            Card(colors = CardDefaults.cardColors(containerColor = if (notice.isRead)
                MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth().clickable {
                onMarkRead(notice.id)
                onOpen(notice)
            }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val icon = when (notice.kind) {
                        NoticeKind.ORDER_CREATED -> Icons.Default.ShoppingBag
                        NoticeKind.ORDER_STATUS -> Icons.Default.CheckCircle
                        NoticeKind.STOCK_LOW -> Icons.Default.WarningAmber
                        NoticeKind.TEST -> Icons.Default.Notifications
                    }
                    Icon(icon, null, Modifier.size(24.dp), tint = if (notice.kind == NoticeKind.STOCK_LOW)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(if (notice.isRead) noticeTitle(notice) else "● ${noticeTitle(notice)}", style = MaterialTheme.typography.labelLarge)
                        Text(noticeMessage(notice), style = MaterialTheme.typography.bodySmall)
                        Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("es", "CL")).format(Date(notice.createdAt)),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (notice.isRead) "Leída" else "Sin leer · Toca para abrir", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { showSettings = !showSettings }, modifier = Modifier.fillMaxWidth()) {
                Text("Avisos en el dispositivo")
                Spacer(Modifier.width(8.dp))
                Icon(if (showSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            if (showSettings) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!permissionGranted) {
                            Text("Activa el permiso para mostrar avisos fuera de la app.",
                                style = MaterialTheme.typography.bodySmall)
                            Button(onClick = onRequestPermission) { Text("Permitir notificaciones") }
                        }
                        Button(onClick = onTest, enabled = !state.busy) {
                            Text("Crear aviso")
                        }
                    }
                }
            }
        }
    }
}
