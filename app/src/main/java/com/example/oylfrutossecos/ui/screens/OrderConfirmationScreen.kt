package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.DeliveryMethod
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.ui.components.priceInPesos
import com.example.oylfrutossecos.ui.viewmodel.CartState
import com.example.oylfrutossecos.ui.viewmodel.OrderUiState

@Composable
fun OrderConfirmationScreen(
    user: User, cart: CartState, state: OrderUiState,
    onBack: () -> Unit, onConfirm: (DeliveryMethod) -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(DeliveryMethod.PICKUP.name) }
    val method = DeliveryMethod.valueOf(selected)
    val pending = cart.pending
    val invalidStock = cart.items.any { it.quantity > it.product.stock }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            TextButton(onClick = onBack, enabled = !state.busy) { Text("← Volver al carrito") }
            Text(if (pending == null) "Confirmar pedido" else "Recuperar pedido pendiente",
                style = MaterialTheme.typography.headlineMedium)
            Text("Pedido de demostración conectado a Spring Boot; sin pago ni compra real.",
                style = MaterialTheme.typography.bodySmall)
        }
        if (cart.loading) item { CircularProgressIndicator() }
        else if (pending != null) item {
            OutlinedCard {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Solicitud: ${pending.requestId}", style = MaterialTheme.typography.bodySmall)
                    Text("Modalidad guardada: ${pending.delivery.displayName}")
                    Text("Se enviará exactamente la solicitud guardada. No se creará otro pedido " +
                        "si el servidor ya lo registró. El stock y los precios actuales no impiden recuperarlo.")
                    if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { onConfirm(pending.delivery) },
                        enabled = !state.busy && cart.canCheckout,
                        modifier = Modifier.fillMaxWidth()) {
                        if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Recuperar pedido pendiente")
                    }
                }
            }
        }
        else if (cart.items.isEmpty()) item {
            Text("Tu carrito está vacío. Regresa al catálogo para agregar productos.")
        } else {
            item {
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resumen", style = MaterialTheme.typography.titleLarge)
                        cart.items.forEach { line ->
                            Text("${line.quantity} × ${line.product.name} — ${priceInPesos(line.subtotalClp.toInt())}")
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", style = MaterialTheme.typography.titleMedium)
                            Text(priceInPesos(cart.total.toInt()),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            item {
                Text("Modalidad", style = MaterialTheme.typography.titleLarge)
                DeliveryMethod.values().forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = method == option,
                            enabled = !state.busy,
                            onClick = { selected = option.name })
                        Text(option.displayName)
                    }
                }
            }
            item {
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Datos de contacto", style = MaterialTheme.typography.titleMedium)
                        Text(user.name)
                        Text("Teléfono: ${user.phone}")
                        Text(if (method == DeliveryMethod.DELIVERY) "Dirección: ${user.address}"
                            else "Retiro en tienda (dirección a coordinar)")
                    }
                }
            }
            item {
                Text("Spring Boot verificará las existencias y descontará stock en H2 de prueba. " +
                    "El total final se calculará con los precios del servidor. " +
                    "Si falla la conexión, podrás reintentar la misma solicitud.",
                    style = MaterialTheme.typography.bodySmall)
                if (invalidStock) Text("Hay productos sin stock suficiente. Vuelve al carrito.",
                    color = MaterialTheme.colorScheme.error)
                if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onConfirm(method) },
                    enabled = !state.busy && cart.canCheckout,
                    modifier = Modifier.fillMaxWidth()) {
                    if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Confirmar pedido en servidor")
                }
            }
        }
    }
}
