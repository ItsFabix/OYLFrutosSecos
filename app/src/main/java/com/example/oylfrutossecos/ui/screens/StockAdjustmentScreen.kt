package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.MovementType
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.domain.InventoryRules
import com.example.oylfrutossecos.ui.viewmodel.InventoryState

@Composable
fun StockAdjustmentScreen(product: Product, state: InventoryState,
                          onBack: () -> Unit, onSave: (MovementType, Int, String) -> Unit) {
    var type by rememberSaveable(product.id) { mutableStateOf(MovementType.ENTRY.name) }
    var quantity by rememberSaveable(product.id) { mutableStateOf("") }
    var note by rememberSaveable(product.id) { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val selected = MovementType.valueOf(type)
    val parsed = quantity.toIntOrNull()
    val projected = if (parsed != null) runCatching {
        InventoryRules.newStock(product.stock, selected, parsed)
    }.getOrNull() else null

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack, enabled = !state.busy) { Text("← Volver al inventario") }
        Text("Registrar movimiento", style = MaterialTheme.typography.headlineMedium)
        Text(product.name, style = MaterialTheme.typography.titleLarge)
        Text("Existencia actual: ${product.stock} bolsas", color = MaterialTheme.colorScheme.primary)
        Text("La entrada o salida se guardará solamente en esta app de demostración.",
            style = MaterialTheme.typography.bodySmall)
        Text("Tipo de movimiento", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selected == MovementType.ENTRY,
                onClick = { type = MovementType.ENTRY.name; localError = null },
                enabled = !state.busy, label = { Text("+ Entrada") })
            FilterChip(selected = selected == MovementType.EXIT,
                onClick = { type = MovementType.EXIT.name; localError = null },
                enabled = !state.busy, label = { Text("− Salida") })
        }
        OutlinedTextField(value = quantity, onValueChange = {
            quantity = it.filter(Char::isDigit).take(6); localError = null
        }, modifier = Modifier.fillMaxWidth(), enabled = !state.busy,
            label = { Text("Cantidad de bolsas") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(value = note, onValueChange = { note = it.take(120); localError = null },
            modifier = Modifier.fillMaxWidth(), enabled = !state.busy, minLines = 2,
            label = { Text("Motivo (ej.: reposición, merma, venta local)") })
        if (projected != null) Text("Nuevo stock estimado: $projected bolsas",
            color = MaterialTheme.colorScheme.primary)
        if (localError != null) Text(localError.orEmpty(), color = MaterialTheme.colorScheme.error)
        if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error)
        Button(modifier = Modifier.fillMaxWidth(), enabled = !state.busy, onClick = {
            localError = when {
                parsed == null -> "Ingresa una cantidad numérica válida."
                else -> runCatching {
                    InventoryRules.newStock(product.stock, selected, parsed)
                    InventoryRules.validateNote(note)?.let { throw IllegalArgumentException(it) }
                }.exceptionOrNull()?.message
            }
            if (localError == null && parsed != null) onSave(selected, parsed, note)
        }) { Text(if (state.busy) "Guardando…" else "Guardar movimiento") }
    }
}
