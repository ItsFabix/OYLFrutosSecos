package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.OrderStatus
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.data.model.UserRole
import com.example.oylfrutossecos.domain.InventoryRules
import com.example.oylfrutossecos.ui.components.ProductCard
import com.example.oylfrutossecos.ui.viewmodel.CartState
import com.example.oylfrutossecos.ui.viewmodel.CatalogState
import com.example.oylfrutossecos.ui.viewmodel.InventoryState
import com.example.oylfrutossecos.ui.viewmodel.OrderUiState

@Composable
fun HomeScreen(
    user: User, state: CatalogState, cartState: CartState, orderState: OrderUiState,
    inventoryState: InventoryState?, onCatalog: () -> Unit, onProduct: (Long) -> Unit,
    onRetry: () -> Unit, onMap: () -> Unit, onNotifications: () -> Unit,
    unreadNotices: Int, onCart: () -> Unit, onOrders: () -> Unit,
    onProducts: () -> Unit, onInventory: () -> Unit, onRetryInventory: () -> Unit,
    navigationEnabled: Boolean
) {
    val admin = user.role == UserRole.ADMIN
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("OYL · Frutos Secos", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text("Hola, ${user.name}", style = MaterialTheme.typography.headlineMedium)
            Text(if (admin) "Resumen administrativo" else "Descubre nuestros productos")
        }
        if (admin) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Datos locales de demostración", style = MaterialTheme.typography.titleMedium)
                        Text("Inventario guardado en este dispositivo e historial local; no representan una consulta sincronizada del backend.",
                            style = MaterialTheme.typography.bodySmall)
                        val products = inventoryState?.takeIf { !it.loading && it.error == null }?.products
                        val pending = orderState.takeIf { !it.loading && it.error == null }
                            ?.orders?.count { it.status == OrderStatus.PENDING }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HomeMetric("Productos", products?.size, Icons.Default.Inventory2, Modifier.weight(1f))
                            HomeMetric("Agotados", products?.count { it.stock == 0 }, Icons.Default.RemoveShoppingCart, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HomeMetric("Stock bajo (1–${InventoryRules.LOW_STOCK_LIMIT})",
                                products?.count { it.stock in 1..InventoryRules.LOW_STOCK_LIMIT },
                                Icons.Default.WarningAmber, Modifier.weight(1f))
                            HomeMetric("Pedidos pendientes", pending, Icons.AutoMirrored.Filled.ReceiptLong, Modifier.weight(1f))
                        }
                        if (inventoryState?.loading == true || orderState.loading) Text("Cargando resumen local…")
                        if (inventoryState == null || inventoryState.error != null) {
                            Text("Inventario local no disponible.", color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetryInventory, enabled = navigationEnabled) { Text("Reintentar inventario") }
                        }
                        if (orderState.error != null) Text("Historial local no disponible. Reintenta desde Pedidos.",
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeAccess("Administrar productos", Icons.Default.Inventory2, navigationEnabled, onProducts)
                    HomeAccess("Inventario", Icons.Default.Warehouse, navigationEnabled, onInventory)
                    HomeAccess("Pedidos", Icons.AutoMirrored.Filled.ReceiptLong, navigationEnabled, onOrders)
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Tu carrito", style = MaterialTheme.typography.titleLarge)
                        when {
                            cartState.loading -> Text("Cargando carrito…")
                            cartState.error != null -> Text("No se pudo actualizar el resumen. Revisa el carrito.",
                                color = MaterialTheme.colorScheme.error)
                            cartState.items.isEmpty() -> Text("Tu carrito está vacío.")
                            else -> Text("${cartState.count} bolsas · Total: CLP ${cartState.total}")
                        }
                        if (cartState.pending != null) Text("Tienes una compra pendiente de recuperación. Ábrela desde el carrito.")
                        Button(onClick = onCart, enabled = navigationEnabled) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ver carrito")
                        }
                    }
                }
            }
            item { HomeAccess("Mis pedidos", Icons.AutoMirrored.Filled.ReceiptLong, navigationEnabled, onOrders) }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeAccess("Explorar catálogo", Icons.Default.ShoppingBag, navigationEnabled, onCatalog)
                HomeAccess("Notificaciones" + if (unreadNotices > 0) " ($unreadNotices nuevas)" else "",
                    Icons.Default.Notifications, navigationEnabled, onNotifications)
                HomeAccess("Mapa y puntos de retiro (demo)", Icons.Default.LocationOn, navigationEnabled, onMap)
            }
        }
        if (!admin) {
            item {
                Text("Productos destacados", style = MaterialTheme.typography.titleLarge)
                Text("Productos, precios y existencias de demostración.", style = MaterialTheme.typography.bodySmall)
            }
            when {
                state.loading -> item { CircularProgressIndicator() }
                state.error != null -> item {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry, enabled = navigationEnabled) { Text("Reintentar catálogo") }
                }
                else -> {
                    val featured = state.products.filter { it.stock > 0 }.take(3)
                    if (featured.isEmpty()) item { Text("No hay productos disponibles para destacar.") }
                    items(featured, key = { it.id }) { product ->
                        ProductCard(product, { if (navigationEnabled) onProduct(product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMetric(label: String, value: Int?, icon: ImageVector, modifier: Modifier) {
    OutlinedCard(modifier.semantics(mergeDescendants = true) {}) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value?.toString() ?: "—", style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun HomeAccess(label: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
