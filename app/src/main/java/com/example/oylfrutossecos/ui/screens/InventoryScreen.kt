package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.domain.StorePresentation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.InventoryMovement
import com.example.oylfrutossecos.domain.InventoryRules
import com.example.oylfrutossecos.ui.viewmodel.InventoryState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InventoryScreen(state: InventoryState, onBack: () -> Unit,
                    onRefresh: () -> Unit, onAdjust: (Long) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var lowOnly by rememberSaveable { mutableStateOf(false) }
    val lowCount = state.products.count { it.stock <= InventoryRules.LOW_STOCK_LIMIT }
    val displayed = state.products.filter { (!lowOnly || it.stock <= InventoryRules.LOW_STOCK_LIMIT) && it.name.contains(query, true) }

    LazyColumn(Modifier.fillMaxSize().testTag("inventory-list"), contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ReferenceTitle("Gestión de inventario", onBack)
            OutlinedTextField(query, { query = it }, label = { Text("Buscar productos") },
                leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("${quantityLabel(state.products.size, "producto", "productos")} · $lowCount con stock bajo o agotado",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OylFilterChip(selected = !lowOnly, onClick = { lowOnly = false },
                    label = { Text("Todos") })
                OylFilterChip(selected = lowOnly, onClick = { lowOnly = true },
                    label = { Text("Stock bajo ($lowCount)") })
            }
            TextButton(onClick = onRefresh) { Text("Actualizar inventario") }
        }
        if (state.loading) item { CircularProgressIndicator() }
        if (state.error != null) item {
            Text(storeMessage(state.error), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRefresh) { Text("Reintentar") }
        }
        if (state.notice != null) item { Text(state.notice, color = MaterialTheme.colorScheme.primary) }
        if (!state.loading && displayed.isEmpty()) item {
            EmptyState("Inventario al día", if (lowOnly) "No hay productos con stock bajo." else "Crea un producto para comenzar.")
        }
        items(displayed, key = { it.id }) { product ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.small) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    ProductImage(product, Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(product.name, style = MaterialTheme.typography.labelLarge)
                        Text("Stock actual: ${product.stock}", style = MaterialTheme.typography.bodySmall,
                            color = if (product.stock <= InventoryRules.LOW_STOCK_LIMIT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        Text("Stock bajo: ${InventoryRules.LOW_STOCK_LIMIT} o menos", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { onAdjust(product.id) }, enabled = !state.busy) {
                        Icon(Icons.Default.SwapVert, "Registrar entrada / salida de ${product.name}",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("Últimos movimientos", style = MaterialTheme.typography.titleLarge)
            Text("Hasta 100 movimientos, del más reciente al más antiguo.",
                style = MaterialTheme.typography.bodySmall)
        }
        if (!state.loading && state.movements.isEmpty()) item {
            Text("Todavía no se registran entradas ni salidas.")
        }
        items(state.movements, key = { "movement-${it.id}" }) { movement ->
            InventoryMovementCard(movement)
        }
    }
}

@Composable
private fun InventoryMovementCard(movement: InventoryMovement) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${movement.type.label}: ${movement.productName}",
                style = MaterialTheme.typography.titleSmall)
            Text("${if (movement.type.name == "ENTRY") "+" else "−"}${quantityLabel(movement.quantity, "bolsa", "bolsas")} · ${movement.beforeStock} → ${movement.afterStock}")
            Text("Motivo: ${StorePresentation.inventoryNote(movement.note)}")
            Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("es", "CL"))
                .format(Date(movement.createdAt)), style = MaterialTheme.typography.bodySmall)
        }
    }
}
