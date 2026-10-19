package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.ui.components.ProductCard
import com.example.oylfrutossecos.ui.viewmodel.CatalogState

@Composable
fun CatalogScreen(state: CatalogState, onSearch: (String) -> Unit,
    onCategory: (String?) -> Unit, onClear: () -> Unit,
    onRetry: () -> Unit, onProduct: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Catálogo", style = MaterialTheme.typography.headlineMedium)
            Text("Encuentra tus favoritos", style = MaterialTheme.typography.bodyLarge)
        }
        item {
            OutlinedTextField(value = state.query, onValueChange = onSearch,
                label = { Text("Buscar por nombre o categoría") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (state.query.isNotEmpty()) TextButton(onClick = { onSearch("") }) { Text("Borrar") }
                })
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.category == null, onClick = { onCategory(null) },
                    label = { Text("Todas") })
                state.categories.forEach { category ->
                    FilterChip(selected = state.category == category, onClick = { onCategory(category) },
                        label = { Text(category) })
                }
            }
        }
        item { Text("Catálogo de demostración · precios y stock de prueba",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        when {
            state.loading -> item { CircularProgressIndicator() }
            state.error != null -> item {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text("Reintentar") }
            }
            state.visibleProducts.isEmpty() -> item {
                Text("No encontramos productos con esos filtros.")
                TextButton(onClick = onClear) { Text("Limpiar filtros") }
            }
            else -> {
                item { Text("${state.visibleProducts.size} productos", style = MaterialTheme.typography.labelLarge) }
                items(state.visibleProducts, key = { it.id }) { product ->
                    ProductCard(product, onClick = { onProduct(product.id) })
                }
            }
        }
    }
}
