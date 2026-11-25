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
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.ui.viewmodel.OrderUiState
import com.example.oylfrutossecos.data.remote.OylRestClient
import com.example.oylfrutossecos.data.remote.ApiOrderEvent
import com.example.oylfrutossecos.domain.StorePresentation
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersScreen(state: OrderUiState, admin: Boolean, onBack: () -> Unit,
    onRetry: () -> Unit, onAdvance: (OrderSummary) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val shown = state.orders.filter { filter == "ALL" || it.status.name == filter }
    LazyColumn(Modifier.fillMaxSize().testTag("orders-list"), contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ReferenceTitle(if (admin) "Gestionar pedidos" else "Mis pedidos", onBack)
            TextButton(onClick = onRetry, enabled = !state.busy && !state.loading) {
                Text("Actualizar pedidos")
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OylFilterChip(selected = filter == "ALL", onClick = { filter = "ALL" }, label = { Text("Todos") })
                OrderStatus.values().forEach { status ->
                    OylFilterChip(selected = filter == status.name,
                        onClick = { filter = status.name }, label = { Text(status.displayName) })
                }
            }
        }
        if (state.loading) item { CircularProgressIndicator() }
        if (state.error != null) item {
            Text(storeMessage(state.error), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Reintentar") }
        }
        if (!state.loading && shown.isEmpty()) item {
            EmptyState("Sin pedidos por aquí", "Los pedidos que coincidan con este estado aparecerán aquí.")
        }
        items(shown, key = { it.remoteOrderId ?: "local-${it.id}" }) { order ->
            var expanded by rememberSaveable(order.id) { mutableStateOf(false) }
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(StorePresentation.orderTitle(order),
                                style = MaterialTheme.typography.labelLarge)
                            Text(SimpleDateFormat("dd/MM/yyyy", Locale("es", "CL")).format(Date(order.createdAt)),
                                style = MaterialTheme.typography.labelSmall)
                            if (admin) Text(order.customerName, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(Modifier.widthIn(max = 145.dp), horizontalAlignment = Alignment.End) {
                            Text(priceInPesos(order.totalClp), style = MaterialTheme.typography.titleSmall)
                            OrderStatusBadge(order.status)
                        }
                    }
                    TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar detalle" else "Ver detalle") }
                    if (expanded) {
                        Text(if (order.remoteOrderId == null) "Pedido anterior · guardado en este dispositivo" else "Pedido registrado",
                            style = MaterialTheme.typography.bodySmall)
                        Text(order.delivery.displayName, style = MaterialTheme.typography.bodySmall)
                        if (admin) {
                            Text("Teléfono: ${order.contactPhone}", style = MaterialTheme.typography.bodySmall)
                            Text("Dirección/retiro: ${order.contactAddress}", style = MaterialTheme.typography.bodySmall)
                        }
                        order.lines.forEach { line -> Text("${line.quantity} × ${line.name} · ${priceInPesos(line.subtotalClp)}",
                            style = MaterialTheme.typography.bodySmall) }
                        order.remoteOrderId?.let { remoteId ->
                            RemoteOrderEvents(remoteId, order.status, order.delivery)
                        }
                    }
                    if (admin && order.status.next() != null) {
                        FilledTonalButton(onClick = { onAdvance(order) },
                            enabled = !state.busy && !state.loading && (order.remoteOrderId == null || state.remoteFresh), modifier = Modifier.fillMaxWidth()) {
                            Text("Marcar como ${order.status.next()?.displayName}")
                        }
                    }
                }
            }
        }
    }
}

/** Evidencia de integración Android -> ventas :8080 -> notificaciones :8082. */
@Composable
private fun RemoteOrderEvents(orderId: String, status: OrderStatus, delivery: DeliveryMethod) {
    var error by remember(orderId, status) { mutableStateOf(false) }
    var retry by remember(orderId) { mutableIntStateOf(0) }
    val events by produceState<List<ApiOrderEvent>?>(initialValue = null, orderId, status, retry) {
        error = false
        value = null
        value = try {
            OylRestClient.service.orderEvents(orderId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = true
            emptyList()
        }
    }
    HorizontalDivider()
    Text("Seguimiento del pedido", style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary)
    when {
        events == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
        error -> {
            Text("No pudimos actualizar el seguimiento. Tu pedido sigue registrado.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { retry++ }) { Text("Reintentar seguimiento") }
        }
        events!!.isEmpty() -> Text("El seguimiento de tu pedido aparecerá aquí.",
            style = MaterialTheme.typography.bodySmall)
        else -> events!!.distinctBy { it.eventId }.sortedBy { it.createdAt }.forEach { event ->
            Text("• ${StorePresentation.eventTitle(event.status)}", style = MaterialTheme.typography.labelMedium)
            Text(StorePresentation.eventMessage(event.status, delivery.name), style = MaterialTheme.typography.bodySmall)
            Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("es", "CL")).format(Date(event.createdAt)),
                style = MaterialTheme.typography.labelSmall)
        }
    }
}
