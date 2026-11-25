package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.OrderStatus
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.data.model.UserRole
import com.example.oylfrutossecos.domain.InventoryRules
import com.example.oylfrutossecos.ui.components.*
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
    LazyColumn(Modifier.fillMaxSize().testTag("home-list"), contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hola, ${user.name}", style = MaterialTheme.typography.titleLarge)
                    Text(if (admin) "Resumen administrativo" else "Descubre nuestros productos",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onNotifications, enabled = navigationEnabled) {
                    BadgedBox(badge = { if (unreadNotices > 0) Badge { Text("$unreadNotices") } }) {
                        Icon(Icons.Default.Notifications, "Notificaciones")
                    }
                }
            }
        }
        if (admin) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.SpaceDashboard, null, Modifier.size(22.dp))
                            Column {

                                Text("Resumen general", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        val products = state.takeIf { !it.loading && it.error == null }?.products
                        val pending = orderState.takeIf { !it.loading && it.error == null && it.remoteFresh }
                            ?.orders?.count { it.remoteOrderId != null && it.status == OrderStatus.PENDING }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HomeMetric("Productos", products?.size, Icons.Default.Inventory2, Modifier.weight(1f))
                                HomeMetric("Agotados", products?.count { it.stock == 0 }, Icons.Default.RemoveShoppingCart, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HomeMetric("Stock bajo (1–${InventoryRules.LOW_STOCK_LIMIT})",
                                    products?.count { it.stock in 1..InventoryRules.LOW_STOCK_LIMIT },
                                    Icons.Default.WarningAmber, Modifier.weight(1f))
                                HomeMetric("Pedidos pendientes", pending, Icons.AutoMirrored.Filled.ReceiptLong, Modifier.weight(1f))
                            }
                        }
                        if (inventoryState?.loading == true || orderState.loading) Text("Cargando resumen…")
                    }
                }
            }
            item { Text("Controla tus productos y pedidos", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (inventoryState == null || inventoryState.error != null) item {
                InfoBanner("Inventario no disponible", inventoryState?.error ?: "No pudimos actualizar las existencias. Inténtalo nuevamente.", error = true)
                TextButton(onClick = onRetryInventory, enabled = navigationEnabled) { Text("Reintentar inventario") }
            }
            if (orderState.error != null) item {
                InfoBanner("Historial no disponible", orderState.error, error = true)
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Box(Modifier.fillMaxWidth().height(108.dp)) {
                        OylArtwork(Modifier.fillMaxHeight().fillMaxWidth(.5f).align(androidx.compose.ui.Alignment.CenterEnd),
                            bowl = true, onDark = true)
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(
                            MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = .08f)))))
                        Column(Modifier.fillMaxWidth(.68f).align(androidx.compose.ui.Alignment.CenterStart).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Compra saludable,\nvida mejor", color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleMedium)
                            Text("Frutos secos, semillas y más", color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Tu carrito", style = MaterialTheme.typography.labelLarge)
                            Text(when {
                                cartState.loading -> "Cargando carrito…"
                                cartState.error != null -> "Revisa el carrito para actualizar"
                                cartState.items.isEmpty() -> "Tu carrito está vacío."
                                else -> "${quantityLabel(cartState.count, "bolsa", "bolsas")} · Total: ${priceInPesos(cartState.total)}"
                            }, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = onCart, enabled = navigationEnabled) { Text("Ver carrito") }
                    }
                }
            }
            if (cartState.pending != null) item {
                InfoBanner("Solicitud anterior protegida",
                    "Hay un pedido anterior por revisar. Abre el carrito para conocer su estado.", error = true)
            }
        }
        item { Text("Accesos rápidos", style = MaterialTheme.typography.titleMedium) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (admin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShortcutTile("Gestionar productos", Icons.Default.Inventory2, navigationEnabled, Modifier.weight(1f), onProducts)
                        ShortcutTile("Inventario", Icons.Default.Warehouse, navigationEnabled, Modifier.weight(1f), onInventory)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShortcutTile("Pedidos", Icons.AutoMirrored.Filled.ReceiptLong, navigationEnabled, Modifier.weight(1f), onOrders)
                        ShortcutTile("Explorar catálogo", Icons.Default.ShoppingBag, navigationEnabled, Modifier.weight(1f), onCatalog)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShortcutTile("Explorar catálogo", Icons.Default.ShoppingBag, navigationEnabled, Modifier.weight(1f), onCatalog)
                        ShortcutTile("Mis pedidos", Icons.AutoMirrored.Filled.ReceiptLong, navigationEnabled, Modifier.weight(1f), onOrders)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShortcutTile("Notificaciones" + if (unreadNotices > 0) " ($unreadNotices ${if (unreadNotices == 1) "nueva" else "nuevas"})" else "",
                        Icons.Default.Notifications, navigationEnabled, Modifier.weight(1f), onNotifications)
                    ShortcutTile("Puntos de retiro", Icons.Default.LocationOn, navigationEnabled, Modifier.weight(1f), onMap)
                }
            }
        }
        if (!admin) {
            item { Text("Productos destacados", style = MaterialTheme.typography.titleMedium) }
            when {
                state.loading -> item { CircularProgressIndicator() }
                state.error != null -> item {
                    InfoBanner("Catálogo no disponible", state.error, error = true)
                    TextButton(onClick = onRetry, enabled = navigationEnabled) { Text("Reintentar catálogo") }
                }
                else -> {
                    val featured = state.products.filter { it.stock > 0 }.take(3)
                    if (featured.isEmpty()) item { EmptyState("Pronto habrá más", "No hay productos disponibles para destacar.") }
                    item {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val tileWidth = (maxWidth - 16.dp) / 3
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                featured.forEach { product ->
                                    Card(onClick = { if (navigationEnabled) onProduct(product.id) },
                                        modifier = Modifier.width(tileWidth), shape = MaterialTheme.shapes.small,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                                        ProductImage(product, Modifier.fillMaxWidth().height(66.dp))
                                        Column(Modifier.padding(6.dp)) {
                                            Text(product.name, style = MaterialTheme.typography.labelSmall, maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, minLines = 2)
                                            Text(priceInPesos(product.priceClp), style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMetric(label: String, value: Int?, icon: ImageVector, modifier: Modifier) {
    Surface(modifier.semantics(mergeDescendants = true) {}, shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.onPrimary) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {

            Text(value?.toString() ?: "—", style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 2)
        }
    }
}
