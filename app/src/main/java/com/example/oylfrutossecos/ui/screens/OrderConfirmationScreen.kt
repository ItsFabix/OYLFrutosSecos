package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.DeliveryMethod
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.ui.viewmodel.CartState
import com.example.oylfrutossecos.ui.viewmodel.OrderUiState

@Composable
fun OrderConfirmationScreen(user: User, cart: CartState, state: OrderUiState,
    onBack: () -> Unit, onConfirm: (DeliveryMethod) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(DeliveryMethod.PICKUP.name) }
    val method = DeliveryMethod.valueOf(selected)
    LazyColumn(Modifier.fillMaxSize().testTag("confirmation-list"), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            TextButton(onClick = onBack, enabled = !state.busy) { Text("← Volver al carrito") }
            CompactHeader(if (cart.pending == null) "Confirmar pedido" else "Solicitud anterior protegida",
                "Revisa tus productos y los datos del pedido.")
        }
        when {
            cart.loading -> item { CircularProgressIndicator() }
            cart.pending != null -> item {
                InfoBanner("Resultado pendiente",
                    "Comprobaremos tu solicitud anterior para evitar una compra duplicada. Tu carrito se mantendrá protegido hasta conocer el resultado.", error = true)
                state.error?.let { Text(storeMessage(it), color = MaterialTheme.colorScheme.error) }
                Button(onClick = { onConfirm(cart.pending.delivery) }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(if (state.busy) "Recuperando…" else "Recuperar pedido pendiente")
                }
            }
            cart.items.isEmpty() -> item { EmptyState("Tu carrito está vacío", "Regresa al catálogo para elegir productos.", Icons.AutoMirrored.Filled.ReceiptLong) }
            else -> {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Resumen", style = MaterialTheme.typography.titleLarge)
                            cart.items.forEach { line ->
                                Text("${line.quantity} × ${line.product.name} — ${priceInPesos(line.subtotalClp)}")
                            }
                            HorizontalDivider()
                            Text("Total", style = MaterialTheme.typography.labelLarge)
                            Text(priceInPesos(cart.total), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item {
                    Text("Modalidad", style = MaterialTheme.typography.titleMedium)
                    DeliveryMethod.values().forEach { option ->
                        Row(Modifier.fillMaxWidth().selectable(selected = method == option, enabled = !state.busy,
                            role = Role.RadioButton, onClick = { selected = option.name }).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = method == option, enabled = !state.busy, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(option.displayName)
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Datos de contacto", style = MaterialTheme.typography.titleMedium)
                            Text(user.name)
                            Text("Teléfono: ${user.phone}")
                            Text(if (method == DeliveryMethod.DELIVERY) "Dirección: ${user.address}" else "Retiro en tienda (a coordinar)")
                        }
                    }
                }
                item { InfoBanner("Antes de confirmar",
                    "Comprobaremos los precios y la disponibilidad al registrar el pedido. Esta versión académica no realiza cobros, despachos ni reservas de retiro reales.") }
                item {
                    if (cart.items.any { it.quantity > it.product.stock }) Text("Hay productos sin stock suficiente. Vuelve al carrito.", color = MaterialTheme.colorScheme.error)
                    state.error?.let { Text(storeMessage(it), color = MaterialTheme.colorScheme.error) }
                    Button(onClick = { onConfirm(method) }, enabled = !state.busy && cart.canCheckout,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        if (state.busy) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Guardando pedido…")
                        } else Text("Registrar pedido")
                    }
                }
            }
        }
    }
}
