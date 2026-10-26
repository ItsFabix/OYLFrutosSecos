package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
    var lowOnly by rememberSaveable { mutableStateOf(false) }
    val lowCount = state.products.count { it.stock <= InventoryRules.LOW_STOCK_LIMIT }
    val displayed = state.products.filter { !lowOnly || it.stock <= InventoryRules.LOW_STOCK_LIMIT }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) { Text("← Volver") }
            Text("Gestión de inventario", style = MaterialTheme.typography.headlineMedium)
            Text("Administrador · inventario local",
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            OutlinedCard {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Resumen de existencias", style = MaterialTheme.typography.titleMedium)
                    Text("${state.products.size} productos · $lowCount con stock bajo o agotado")
                    Text("Stock bajo: 5 bolsas o menos.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !lowOnly, onClick = { lowOnly = false },
                    label = { Text("Todos") })
                FilterChip(selected = lowOnly, onClick = { lowOnly = true },
                    label = { Text("Stock bajo ($lowCount)") })
            }
            TextButton(onClick = onRefresh) { Text("Actualizar inventario") }
        }
        if (state.loading) item { CircularProgressIndicator() }
        if (state.error != null) item {
            Text(state.error, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRefresh) { Text("Reintentar") }
        }
        if (state.notice != null) item { Text(state.notice, color = MaterialTheme.colorScheme.primary) }
        if (!state.loading && displayed.isEmpty()) item {
            Text(if (lowOnly) "No hay productos con stock bajo." else "No hay productos.")
        }
        items(displayed, key = { it.id }) { product ->
            OutlinedCard {
                Column(Modifier.fillMaxWidth().padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    Text("${product.category} · ${product.weightGrams} g")
                    Text(if (product.stock == 0) "AGOTADO: 0 bolsas" else "Stock actual: ${product.stock} bolsas",
                        color = if (product.stock <= InventoryRules.LOW_STOCK_LIMIT)
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Button(onClick = { onAdjust(product.id) }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()) { Text("Registrar entrada / salida") }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("Últimos movimientos", style = MaterialTheme.typography.titleLarge)
            Text("Hasta 100 registros locales, ordenados del más reciente al más antiguo.",
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
    OutlinedCard {
        Column(Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${movement.type.label}: ${movement.productName}",
                style = MaterialTheme.typography.titleSmall)
            Text("${if (movement.type.name == "ENTRY") "+" else "−"}${movement.quantity} bolsas · ${movement.beforeStock} → ${movement.afterStock}")
            Text("Motivo: ${movement.note}")
            Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("es", "CL"))
                .format(Date(movement.createdAt)), style = MaterialTheme.typography.bodySmall)
        }
    }
}
