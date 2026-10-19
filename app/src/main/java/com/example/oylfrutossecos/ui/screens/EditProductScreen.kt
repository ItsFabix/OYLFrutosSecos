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
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.data.model.ProductDraft

@Composable
fun EditProductScreen(product: Product?, isNew: Boolean, onBack: () -> Unit,
    onSave: (ProductDraft, (String?) -> Unit) -> Unit) {
    var name by rememberSaveable(product?.id) { mutableStateOf(product?.name.orEmpty()) }
    var category by rememberSaveable(product?.id) { mutableStateOf(product?.category ?: "Frutos secos") }
    var description by rememberSaveable(product?.id) { mutableStateOf(product?.description.orEmpty()) }
    var price by rememberSaveable(product?.id) { mutableStateOf(product?.priceClp?.toString().orEmpty()) }
    var grams by rememberSaveable(product?.id) { mutableStateOf(product?.weightGrams?.toString().orEmpty()) }
    var stock by rememberSaveable(product?.id) { mutableStateOf(product?.stock?.toString() ?: "0") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack, enabled = !busy) { Text("Volver") }
        Text(if (isNew) "Nuevo producto" else "Editar producto",
            style = MaterialTheme.typography.headlineMedium)
        Text("Los cambios se guardan en SQLite en este teléfono. No modifican inventario externo.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(), enabled = !busy)
        Text("Categoría", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Frutos secos", "Semillas", "Deshidratados").forEach { option ->
                FilterChip(selected = category == option, onClick = { category = option },
                    label = { Text(option) }, enabled = !busy)
            }
        }
        OutlinedTextField(description, { description = it },
            label = { Text("Descripción") }, minLines = 3,
            modifier = Modifier.fillMaxWidth(), enabled = !busy)
        OutlinedTextField(price, { price = it.filter(Char::isDigit) },
            label = { Text("Precio en pesos") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), enabled = !busy)
        OutlinedTextField(grams, { grams = it.filter(Char::isDigit) },
            label = { Text("Peso en gramos") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), enabled = !busy)
        OutlinedTextField(stock, { stock = it.filter(Char::isDigit) },
            label = { Text("Stock de bolsas") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), enabled = !busy)
        if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
            val unitPrice = price.toIntOrNull()
            val weight = grams.toIntOrNull()
            val quantity = stock.toIntOrNull()
            if (unitPrice == null || weight == null || quantity == null) {
                error = "Ingresa valores numéricos válidos en precio, peso y stock."
            } else {
                val draft = ProductDraft(name, category, description, unitPrice, weight, quantity)
                error = draft.validationError()
                if (error == null) {
                    busy = true
                    onSave(draft) { message ->
                        busy = false
                        error = message
                        if (message == null) onBack()
                    }
                }
            }
        }) { Text(if (busy) "Guardando…" else "Guardar producto") }
    }
}
