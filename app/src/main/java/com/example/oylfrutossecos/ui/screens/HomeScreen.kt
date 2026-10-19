package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.data.model.UserRole
import com.example.oylfrutossecos.ui.components.ProductCard
import com.example.oylfrutossecos.ui.viewmodel.CatalogState

@Composable
fun HomeScreen(user: User, state: CatalogState, onCatalog: () -> Unit,
    onProduct: (Long) -> Unit, onRetry: () -> Unit, onMap: () -> Unit,
    onNotifications: () -> Unit, unreadNotices: Int) {
    val admin = user.role == UserRole.ADMIN
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("OYL · Frutos Secos", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(10.dp))
            Text("Hola, ${user.name}", style = MaterialTheme.typography.headlineMedium)
            Text(if (admin) "Resumen del catálogo" else "Descubre nuestros productos")
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (admin) "Tu catálogo, en un solo lugar" else "Un favorito para cada momento",
                        style = MaterialTheme.typography.titleLarge)
                    Text("Frutos secos, semillas y deshidratados.")
                    Button(onClick = onCatalog) { Text("Explorar catálogo") }
                }
            }
        }
        item {
            OutlinedButton(onClick = onNotifications, modifier = Modifier.fillMaxWidth()) {
                Text("Notificaciones" + if (unreadNotices > 0) " ($unreadNotices nuevas)" else "")
            }
        }
        item {
            OutlinedButton(onClick = onMap, modifier = Modifier.fillMaxWidth()) {
                Text("Ver mapa y puntos de retiro (demo)")
            }
        }
        item { Text("Demostración con productos, precios y existencias de prueba.",
            style = MaterialTheme.typography.bodySmall) }
        when {
            state.loading -> item { CircularProgressIndicator() }
            state.error != null -> item {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text("Reintentar") }
            }
            else -> {
                if (admin) {
                    item {
                        Text("${state.products.size} productos · ${state.products.count { it.stock == 0 }} agotados",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Stock bajo: ${state.products.count { it.stock in 1..10 }} productos con 10 bolsas o menos.")
                    }
                }
                item { Text(if (admin) "Revisar existencias" else "Productos destacados",
                    style = MaterialTheme.typography.titleLarge) }
                val featured = if (admin) state.products.sortedBy { it.stock }.take(3)
                    else state.products.filter { it.stock > 0 }.take(3)
                items(featured, key = { it.id }) { product -> ProductCard(product, { onProduct(product.id) }) }
            }
        }
    }
}
