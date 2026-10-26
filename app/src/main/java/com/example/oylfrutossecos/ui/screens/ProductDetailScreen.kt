package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.ui.components.ProductImage
import com.example.oylfrutossecos.ui.components.priceInPesos

@Composable
fun ProductDetailScreen(product: Product?, loading: Boolean, error: String?,
    onRetry: () -> Unit, onBack: () -> Unit,
    isAdmin: Boolean = false, onEdit: () -> Unit = {},
    cartBusy: Boolean = false, cartNotice: String? = null, cartError: String? = null,
    onAdd: (Int) -> Unit = {}, onCart: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("Volver") }
        when {
            loading -> CircularProgressIndicator()
            error != null -> {
                Text(error, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text("Reintentar") }
            }
            product == null -> Text("Este producto no está disponible en el catálogo.")
            else -> {
                var quantity by remember(product.id) { mutableIntStateOf(1) }
                ProductImage(product, Modifier.fillMaxWidth().height(220.dp))
                Text(product.category, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge)
                Text(product.name, style = MaterialTheme.typography.headlineMedium)
                Text(priceInPesos(product.priceClp), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall)
                Text("Presentación: bolsa de ${product.weightGrams} g")
                Text(if (product.stock == 0) "Producto agotado" else "Disponible: ${product.stock} bolsas",
                    color = if (product.stock == 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary)
                HorizontalDivider()
                Text("Descripción", style = MaterialTheme.typography.titleMedium)
                Text(product.description)
                Text("Código de producto: ${product.id}", style = MaterialTheme.typography.bodySmall)
                if (isAdmin) {
                    Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                        Text("Editar producto")
                    }
                } else {
                    if (product.stock > 0) {
                        Text("Cantidad", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { if (quantity > 1) quantity-- },
                                enabled = !cartBusy && quantity > 1) { Text("−") }
                            Text("$quantity", modifier = Modifier.padding(top = 12.dp))
                            OutlinedButton(onClick = { if (quantity < product.stock) quantity++ },
                                enabled = !cartBusy && quantity < product.stock) { Text("+") }
                        }
                        Button(onClick = { onAdd(quantity) }, enabled = !cartBusy,
                            modifier = Modifier.fillMaxWidth()) { Text("Agregar al carrito") }
                    }
                    if (cartNotice != null) Text(cartNotice, color = MaterialTheme.colorScheme.primary)
                    if (cartError != null) Text(cartError, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onCart, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver mi carrito")
                    }
                }
                Text("Imagen referencial. Los pedidos no generan pagos ni envíos reales.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
