package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.ui.viewmodel.CatalogState

@Composable
fun CatalogScreen(state: CatalogState, onSearch: (String) -> Unit,
    onCategory: (String?) -> Unit, onClear: () -> Unit,
    onRetry: () -> Unit, onProduct: (Long) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().imePadding().testTag("catalog-list"),
        contentPadding = PaddingValues(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { ReferenceTitle("Catálogo") }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(value = state.query, onValueChange = onSearch,
                label = { Text("Buscar", maxLines = 1) }, placeholder = { Text("Nombre o categoría") }, singleLine = true,
                shape = MaterialTheme.shapes.medium, leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (state.query.isNotEmpty()) TextButton(onClick = { onSearch("") }) { Text("Borrar") } },
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth())
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OylFilterChip(state.category == null, { onCategory(null) }, label = { Text("Todas") })
                state.categories.forEach { category ->
                    OylFilterChip(state.category == category, { onCategory(category) }, label = { Text(category) })
                }
            }
        }
        if (state.error != null) item(span = { GridItemSpan(maxLineSpan) }) {
                Column { InfoBanner("Catálogo no disponible", state.error, error = true)
                    TextButton(onClick = onRetry) { Text("Reintentar") } }
        }
        when {
            state.loading -> item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.visibleProducts.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                Column { EmptyState("Sin coincidencias", "Prueba otro nombre o categoría.")
                    TextButton(onClick = onClear) { Text("Limpiar filtros") } }
            }
            else -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("${quantityLabel(state.visibleProducts.size, "producto", "productos")} en el catálogo", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(state.visibleProducts, key = { it.id }) { product -> ProductCard(product) { onProduct(product.id) } }
            }
        }
    }
}
