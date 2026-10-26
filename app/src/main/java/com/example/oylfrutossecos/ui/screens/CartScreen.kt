package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.ui.components.ProductImage
import com.example.oylfrutossecos.ui.components.priceInPesos
import com.example.oylfrutossecos.ui.viewmodel.CartState

@Composable
fun CartScreen(state: CartState, onChange: (Long, Int) -> Unit,
    onRetry: () -> Unit, onCatalog: () -> Unit,
    onCheckout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Mi carrito", style = MaterialTheme.typography.headlineMedium)
            Text("${state.count} bolsas seleccionadas · guardado para tu cuenta")
        }
        if (state.loading) {
            item { CircularProgressIndicator() }
        }
        if (!state.loading && state.pending != null) item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hay un pedido pendiente de confirmación", style = MaterialTheme.typography.titleMedium)
                    Text("Se recuperará la solicitud guardada, aunque el stock actual haya disminuido. " +
                        "El carrito permanecerá bloqueado hasta conocer el resultado.")
                    Text("Solicitud: ${state.pending.requestId}", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onCheckout, enabled = state.canCheckout,
                        modifier = Modifier.fillMaxWidth()) { Text("Recuperar pedido pendiente") }
                }
            }
        }
        if (!state.loading && state.items.isEmpty() && state.pending == null) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Tu carrito está vacío", style = MaterialTheme.typography.titleLarge)
                        Text("Visita el catálogo y agrega tus productos favoritos.")
                        Button(onClick = onCatalog) { Text("Ir al catálogo") }
                    }
                }
            }
        } else if (!state.loading) {
            items(state.items, key = { it.product.id }) { item ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            ProductImage(item.product, Modifier.size(64.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.product.name, style = MaterialTheme.typography.titleMedium)
                                Text("${priceInPesos(item.product.priceClp)} c/u")
                                Text("Subtotal: ${priceInPesos(item.subtotalClp.toInt())}",
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { onChange(item.product.id, item.quantity - 1) },
                                enabled = state.canEdit) { Text("−") }
                            Text("${item.quantity}")
                            OutlinedButton(onClick = { onChange(item.product.id, item.quantity + 1) },
                                enabled = state.canEdit && item.quantity < item.product.stock) { Text("+") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { onChange(item.product.id, 0) }, enabled = state.canEdit) {
                                Text("Quitar")
                            }
                        }
                        if (state.pending == null && item.quantity > item.product.stock) {
                            Text("El stock disminuyó. Ajusta la cantidad antes de comprar.",
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (state.pending == null) item {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total estimado", style = MaterialTheme.typography.titleLarge)
                    Text(priceInPesos(state.total.toInt()),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Text("El servidor verificará los precios y el stock al confirmar " +
                    "el pedido. No se realizará ningún pago ni envío real.",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onCheckout,
                    enabled = state.canCheckout,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Continuar al pedido")
                }
            }
        }
        if (state.notice != null) item { Text(state.notice, color = MaterialTheme.colorScheme.primary) }
        if (state.error != null) item {
            Text(state.error, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Reintentar") }
        }
    }
}
