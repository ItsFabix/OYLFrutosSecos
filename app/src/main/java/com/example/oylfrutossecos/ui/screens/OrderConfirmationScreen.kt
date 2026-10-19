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
    val invalidStock = cart.items.any { it.quantity > it.product.stock }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            TextButton(onClick = onBack, enabled = !state.busy) { Text("← Volver al carrito") }
            Text("Confirmar pedido", style = MaterialTheme.typography.headlineMedium)
            Text("Solicitud de demostración local; no se realiza un cobro ni una compra real.",
                style = MaterialTheme.typography.bodySmall)
        }
        if (cart.loading) item { CircularProgressIndicator() }
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
                Text("El pedido se guardará con estado Pendiente de validación. El stock no se descuenta: " +
                    "la compra oficial dependerá del servidor Spring Boot.",
                    style = MaterialTheme.typography.bodySmall)
                if (invalidStock) Text("Hay productos sin stock suficiente. Vuelve al carrito.",
                    color = MaterialTheme.colorScheme.error)
                if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onConfirm(method) },
                    enabled = !state.busy && !cart.busy && !invalidStock && cart.items.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()) {
                    if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Guardar solicitud de prueba")
                }
            }
        }
    }
}
