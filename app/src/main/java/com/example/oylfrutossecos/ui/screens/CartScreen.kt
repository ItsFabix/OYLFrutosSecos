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
        } else if (state.items.isEmpty()) {
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
        } else {
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
                                enabled = !state.busy) { Text("−") }
                            Text("${item.quantity}")
                            OutlinedButton(onClick = { onChange(item.product.id, item.quantity + 1) },
                                enabled = !state.busy && item.quantity < item.product.stock) { Text("+") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { onChange(item.product.id, 0) }, enabled = !state.busy) {
                                Text("Quitar")
                            }
                        }
                        if (item.quantity > item.product.stock) {
                            Text("El stock disminuyó. Ajusta la cantidad antes de comprar.",
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total estimado", style = MaterialTheme.typography.titleLarge)
                    Text(priceInPesos(state.total.toInt()),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Text("Los precios son estimativos. Spring Boot validará precio y stock al confirmar " +
                    "el pedido de prueba; no se realizará ningún pago.",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onCheckout,
                    enabled = !state.busy && state.items.isNotEmpty() &&
                        state.items.none { it.quantity > it.product.stock },
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
