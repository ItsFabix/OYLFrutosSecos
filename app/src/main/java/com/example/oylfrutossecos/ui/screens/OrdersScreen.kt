package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.ui.components.priceInPesos
import com.example.oylfrutossecos.ui.viewmodel.OrderUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersScreen(state: OrderUiState, admin: Boolean, onBack: () -> Unit,
    onRetry: () -> Unit, onAdvance: (Long) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val shown = state.orders.filter { filter == "ALL" || it.status.name == filter }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            TextButton(onClick = onBack) { Text("← Volver") }
            Text(if (admin) "Gestionar pedidos" else "Mis pedidos",
                style = MaterialTheme.typography.headlineMedium)
            Text("Historial guardado en este dispositivo. " +
                "Los cambios de estado del servidor no se actualizan automáticamente.",
                style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == "ALL", onClick = { filter = "ALL" }, label = { Text("Todos") })
                OrderStatus.values().forEach { status ->
                    FilterChip(selected = filter == status.name,
                        onClick = { filter = status.name }, label = { Text(status.displayName) })
                }
            }
        }
        if (state.loading) item { CircularProgressIndicator() }
        if (state.error != null) item {
            Text(state.error, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Reintentar") }
        }
        if (!state.loading && shown.isEmpty()) item {
            Card { Text("Aún no hay pedidos con este filtro.", Modifier.padding(18.dp)) }
        }
        items(shown, key = { it.id }) { order ->
            OutlinedCard {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Pedido #${order.id}", style = MaterialTheme.typography.titleLarge)
                    if (order.remoteOrderId != null) {
                        Text("Registrado en el servidor: ${order.remoteOrderId}",
                            style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Pedido guardado solo en este dispositivo",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("es", "CL"))
                        .format(Date(order.createdAt)), style = MaterialTheme.typography.bodySmall)
                    if (admin) Text("Cliente: ${order.customerName}")
                    Text("Estado: ${order.status.displayName}", color = MaterialTheme.colorScheme.primary)
                    Text("Modalidad: ${order.delivery.displayName}")
                    if (admin) {
                        Text("Teléfono: ${order.contactPhone}")
                        Text("Dirección/retiro: ${order.contactAddress}")
                    }
                    HorizontalDivider()
                    order.lines.forEach { line ->
                        Text("${line.quantity} × ${line.name} · ${priceInPesos(line.subtotalClp.toInt())}")
                    }
                    Text("Total: ${priceInPesos(order.totalClp.toInt())}",
                        style = MaterialTheme.typography.titleMedium)
                    if (admin && order.remoteOrderId == null && order.status.next() != null) {
                        Button(onClick = { onAdvance(order.id) }, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Marcar como ${order.status.next()?.displayName}")
                        }
                    }
                }
            }
        }
    }
}
