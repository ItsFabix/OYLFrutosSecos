package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.ui.viewmodel.CatalogState

@Composable
fun ManageProductsScreen(state: CatalogState, onNew: () -> Unit, onEdit: (Long) -> Unit,
    onRefresh: () -> Unit, onInventory: () -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Todos") }
    val shown = state.products.filter { it.name.contains(search, true) &&
        (filter == "Todos" || (filter == "Con stock" && it.stock > 0) || (filter == "Agotados" && it.stock == 0)) }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onNew, modifier = Modifier.weight(1f)) { Text("Crear producto") }
                OutlinedButton(onClick = onInventory, modifier = Modifier.weight(1f)) { Text("Inventario") }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("manage-products-list"),
            contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ReferenceTitle("Gestión de productos") }
            item { OutlinedTextField(search, { search = it }, label = { Text("Buscar productos") },
                leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Todos", "Con stock", "Agotados").forEach { label ->
                    OylFilterChip(filter == label, { filter = label }, label = { Text(label) })
                }
            } }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { item { InfoBanner("Catálogo no disponible", it, error = true)
                TextButton(onClick = onRefresh) { Text("Reintentar") } } }
            if (!state.loading && state.error == null && shown.isEmpty()) item { EmptyState("Sin productos", "Revisa la búsqueda o crea un producto.") }
            items(shown, key = { it.id }) { product ->
                AdminProductCard(product) { onEdit(product.id) }
            }
        }
    }
}
