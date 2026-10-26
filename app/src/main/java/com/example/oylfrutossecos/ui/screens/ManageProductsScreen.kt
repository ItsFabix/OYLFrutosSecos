package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.ui.components.ProductCard
import com.example.oylfrutossecos.ui.viewmodel.CatalogState

@Composable
fun ManageProductsScreen(state: CatalogState, onNew: () -> Unit, onEdit: (Long) -> Unit,
    onRefresh: () -> Unit, onInventory: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Gestión de productos", style = MaterialTheme.typography.headlineMedium)
            Text("Administrador · productos guardados en este dispositivo")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onNew) { Text("+ Crear producto") }
                OutlinedButton(onClick = onInventory) { Text("Inventario") }
            }
        }
        if (state.loading) item { CircularProgressIndicator() }
        if (state.error != null) item {
            Text(state.error, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRefresh) { Text("Reintentar") }
        }
        if (!state.loading) {
            items(state.products, key = { it.id }) { product ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProductCard(product) { onEdit(product.id) }
                    TextButton(onClick = { onEdit(product.id) }) { Text("Editar ${product.name}") }
                }
            }
        }
    }
}
