package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.ui.viewmodel.CartState

@Composable
fun CartScreen(state: CartState, onChange: (Long, Int) -> Unit,
    onRetry: () -> Unit, onCatalog: () -> Unit, onCheckout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("cart-list"), contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ReferenceTitle("Mi carrito (${state.count})") }
        if (state.loading) item { CircularProgressIndicator() }
        if (!state.loading && state.pending != null) item {
            InfoBanner("Solicitud anterior protegida",
                "Tu solicitud anterior aún está pendiente de revisión. El carrito permanece protegido para evitar duplicados. " +
                    "Selecciona Recuperar pedido pendiente para revisar su estado.", error = true)
            Button(onClick = onCheckout, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text("Recuperar pedido pendiente")
            }
        }
        if (!state.loading && state.items.isEmpty() && state.pending == null) {
            item { EmptyState("Tu carrito está vacío", "Elige tus frutos secos favoritos y vuelve aquí para confirmar.", Icons.Default.ShoppingCart) }
            item { Button(onClick = onCatalog, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Ir al catálogo") } }
        } else if (!state.loading) {
            items(state.items, key = { it.product.id }) { item ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.small) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val compact = maxWidth < 350.dp
                        Column(Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ProductImage(item.product, Modifier.size(42.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.product.name, style = MaterialTheme.typography.labelMedium)
                                    Text("${item.product.weightGrams} g", style = MaterialTheme.typography.labelSmall)
                                    Text(priceInPesos(item.product.priceClp), style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                if (!compact) CartQuantityControls(item.quantity, state.canEdit,
                                    item.quantity < item.product.stock,
                                    { onChange(item.product.id, item.quantity - 1) },
                                    { onChange(item.product.id, item.quantity + 1) })
                                IconButton(onClick = { onChange(item.product.id, 0) }, enabled = state.canEdit, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.DeleteOutline, "Quitar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (compact) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Subtotal: ${priceInPesos(item.subtotalClp)}", Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall)
                                CartQuantityControls(item.quantity, state.canEdit, item.quantity < item.product.stock,
                                    { onChange(item.product.id, item.quantity - 1) },
                                    { onChange(item.product.id, item.quantity + 1) })
                            } else Text("Subtotal: ${priceInPesos(item.subtotalClp)}", style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.End))
                            if (state.pending == null && item.quantity > item.product.stock)
                                Text("El stock disminuyó. Ajusta la cantidad antes de continuar.", color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (state.pending == null) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resumen del carrito", style = MaterialTheme.typography.titleMedium)
                        Text("Total estimado", style = MaterialTheme.typography.bodyMedium)
                        Text(priceInPesos(state.total), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Los precios y las existencias se comprobarán al confirmar. No se realizan pagos desde la app.",
                            style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onCheckout, enabled = state.canCheckout, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Icon(Icons.Default.ShoppingBag, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Continuar al pedido")
                        }
                    }
                }
            }
        }
        state.notice?.let { item { InfoBanner("Carrito actualizado", it) } }
        state.error?.let { message -> item {
            InfoBanner("No pudimos actualizar el carrito", message, error = true)
            TextButton(onClick = onRetry) { Text("Reintentar") }
        } }
    }
}

@Composable
private fun RowScope.CartQuantityControls(quantity: Int, editable: Boolean, canIncrease: Boolean,
    onDecrease: () -> Unit, onIncrease: () -> Unit) {
    TextButton(onClick = onDecrease, enabled = editable, modifier = Modifier.size(48.dp),
        contentPadding = PaddingValues(0.dp)) { Text("−") }
    Text("$quantity", style = MaterialTheme.typography.labelLarge)
    TextButton(onClick = onIncrease, enabled = editable && canIncrease, modifier = Modifier.size(48.dp),
        contentPadding = PaddingValues(0.dp)) { Text("+") }
}
