package com.example.oylfrutossecos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.oylfrutossecos.data.model.MovementType
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.domain.InventoryField
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.ui.viewmodel.StockAdjustmentViewModel

@Composable
fun StockAdjustmentScreen(product: Product, form: StockAdjustmentViewModel,
                          onBack: () -> Unit) {
    val state by form.state.collectAsStateWithLifecycle()
    LaunchedEffect(product.stock) { form.updateStock(product.stock) }
    val input = state.input
    BackHandler(enabled = state.saving) {}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack, enabled = !state.locked) { Text("← Volver al inventario") }
        StoreHeader("Registrar movimiento", product.name)
        StockBadge(state.currentStock)
        Text("Las entradas y salidas actualizan las existencias y quedan en el historial.",
            style = MaterialTheme.typography.bodySmall)
        Text("Tipo de movimiento", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MovementType.entries.forEach { option ->
                FilterChip(selected = input.type == option.name,
                    onClick = { form.change(InventoryField.TYPE, option.name) }, enabled = !state.locked,
                    leadingIcon = { Icon(if (option == MovementType.ENTRY) Icons.Default.Add else Icons.Default.Remove,
                        contentDescription = option.label) },
                    label = { Text(option.label) })
            }
        }
        Text(state.errors[InventoryField.TYPE] ?: "Entrada suma bolsas; Salida retira bolsas del stock.",
            color = if (state.errors[InventoryField.TYPE] != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        AdminFormField("Cantidad de bolsas", input.quantity, { form.change(InventoryField.QUANTITY, it) },
            Icons.Default.Inventory2, state.errors[InventoryField.QUANTITY],
            "Entre 1 y 10.000; una salida no puede superar las existencias.", !state.locked, KeyboardType.Number)
        AdminFormField("Motivo", input.note, { form.change(InventoryField.NOTE, it) },
            Icons.Default.EditNote, state.errors[InventoryField.NOTE],
            "De 3 a 120 caracteres. Ejemplos: reposición o merma.", !state.locked, minLines = 2)
        state.projectedStock?.let { Text("Nuevo stock estimado: ${quantityLabel(it, "bolsa", "bolsas")}",
            color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(storeMessage(it), color = MaterialTheme.colorScheme.error) }
        Button(onClick = form::save, enabled = !state.locked, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            if (state.saving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(Icons.Default.Save, contentDescription = "Guardar movimiento de inventario")
                Spacer(Modifier.width(8.dp))
            }
            Text(if (state.saving) "Guardando…" else "Guardar movimiento")
        }
    }
}
