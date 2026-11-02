package com.example.oylfrutossecos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.domain.ProductField
import com.example.oylfrutossecos.domain.ProductValidator
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.ui.viewmodel.ProductFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(product: Product?, form: ProductFormViewModel, onBack: () -> Unit) {
    val state by form.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun discardTemporary(names: List<String>) {
        names.forEach { ProductPhotoStore.delete(context, it) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        discardTemporary(form.finishPhotoCapture(success))
    }
    BackHandler(enabled = state.saving || state.pendingPhoto != null) {}
    LaunchedEffect(state.completed) { if (state.completed) onBack() }
    var categoryExpanded by remember { mutableStateOf(false) }
    val input = state.input
    val enabled = !state.locked

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack, enabled = enabled) { Text("Volver") }
        StoreHeader(if (state.isNew) "Nuevo producto" else "Editar producto",
            "Información y fotografías del catálogo")
        state.imageKey?.let { key ->
            val preview = product?.copy(imageKey = key)
                ?: Product(0, input.name, input.category, input.description, 1, 1, 0, key)
            ProductImage(preview, Modifier.fillMaxWidth().height(170.dp))
        }
        OutlinedButton(onClick = {
            try {
                val (name, uri) = ProductPhotoStore.create(context)
                if (form.beginPhotoCapture(name)) camera.launch(uri)
            } catch (_: Exception) { discardTemporary(form.cameraFailed()) }
        }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoCamera, contentDescription = "Abrir cámara para fotografiar el producto")
            Spacer(Modifier.width(8.dp))
            Text("Tomar fotografía")
        }
        if (state.selectedPhoto != null) TextButton(
            onClick = { discardTemporary(form.discardPhoto()) }, enabled = enabled
        ) { Text("Descartar fotografía nueva") }
        AdminFormField("Nombre", input.name, { form.change(ProductField.NAME, it) },
            Icons.Default.ShoppingBag, state.errors[ProductField.NAME], "Al menos 3 caracteres.", enabled)
        ExposedDropdownMenuBox(expanded = categoryExpanded,
            onExpandedChange = { if (enabled) categoryExpanded = it }) {
            OutlinedTextField(value = input.category, onValueChange = {}, readOnly = true,
                enabled = enabled, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
                label = { Text("Categoría") }, shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = "Categoría del producto") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                isError = state.errors[ProductField.CATEGORY] != null,
                supportingText = { Text(state.errors[ProductField.CATEGORY] ?: "Selecciona una categoría de la lista.") })
            ExposedDropdownMenu(expanded = categoryExpanded && enabled,
                onDismissRequest = { categoryExpanded = false }) {
                ProductValidator.categories.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        form.change(ProductField.CATEGORY, option)
                        categoryExpanded = false
                    })
                }
            }
        }
        AdminFormField("Descripción", input.description, { form.change(ProductField.DESCRIPTION, it) },
            Icons.Default.Description, state.errors[ProductField.DESCRIPTION],
            "Describe el producto con al menos 5 caracteres.", enabled, minLines = 3)
        AdminFormField("Precio en pesos", input.price, { form.change(ProductField.PRICE, it) },
            Icons.Default.Payments, state.errors[ProductField.PRICE],
            "Número entero mayor que cero, sin separadores.", enabled, KeyboardType.Number)
        AdminFormField("Peso en gramos", input.weight, { form.change(ProductField.WEIGHT, it) },
            Icons.Default.Scale, state.errors[ProductField.WEIGHT],
            "Peso entero mayor que cero.", enabled, KeyboardType.Number)
        AdminFormField(if (state.isNew) "Stock inicial de bolsas" else "Stock actual (solo lectura)",
            input.initialStock, { form.change(ProductField.STOCK, it) }, Icons.Default.Inventory2,
            state.errors[ProductField.STOCK],
            if (state.isNew) "Cantidad entera de bolsas; puede ser cero."
            else "Para cambiar existencias usa Productos → Inventario.",
            enabled, KeyboardType.Number, readOnly = !state.isNew)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = form::save, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            if (state.saving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(Icons.Default.Save, contentDescription = "Guardar cambios del producto")
                Spacer(Modifier.width(8.dp))
            }
            Text(if (state.saving) "Guardando…" else "Guardar producto")
        }
    }
}
