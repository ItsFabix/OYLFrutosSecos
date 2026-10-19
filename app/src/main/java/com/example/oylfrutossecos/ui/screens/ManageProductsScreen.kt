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
    onRefresh: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Gestión de productos", style = MaterialTheme.typography.headlineMedium)
            Text("Administrador · datos locales de demostración")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onNew) { Text("+ Crear producto") }
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
