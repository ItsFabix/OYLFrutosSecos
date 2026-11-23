package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.ui.components.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.oylfrutossecos.ui.components.priceInPesos

@Composable
fun ProductDetailScreen(product: Product?, loading: Boolean, error: String?,
    onRetry: () -> Unit, onBack: () -> Unit,
    isAdmin: Boolean = false, onEdit: () -> Unit = {},
    cartBusy: Boolean = false, cartNotice: String? = null, cartError: String? = null,
    onAdd: (Int) -> Unit = {}, onCart: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReferenceTitle("Detalle del producto", onBack)
        when {
            loading -> CircularProgressIndicator()
            error != null -> {
                Text(error, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text("Reintentar") }
            }
            product == null -> Text("Este producto no está disponible en el catálogo.")
            else -> {
                var quantity by rememberSaveable(product.id) { mutableIntStateOf(1) }
                ProductImage(product, Modifier.fillMaxWidth().height(190.dp))
                Text(product.name, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(priceInPesos(product.priceClp), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge)
                    Text("Bolsa de ${product.weightGrams} g", style = MaterialTheme.typography.bodySmall)
                }
                StockBadge(product.stock)
                HorizontalDivider()
                Text("Descripción", style = MaterialTheme.typography.titleMedium)
                Surface(shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    Text(product.description, Modifier.fillMaxWidth().padding(10.dp),
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (isAdmin) {
                    Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                        Text("Editar producto")
                    }
                } else {
                    if (product.stock > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            OutlinedButton(onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier.width(48.dp), contentPadding = PaddingValues(0.dp),
                                enabled = !cartBusy && quantity > 1) { Text("−") }
                            Text("$quantity")
                            OutlinedButton(onClick = { if (quantity < product.stock) quantity++ },
                                modifier = Modifier.width(48.dp), contentPadding = PaddingValues(0.dp),
                                enabled = !cartBusy && quantity < product.stock) { Text("+") }
                            Button(onClick = { onAdd(quantity) }, enabled = !cartBusy,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(6.dp)) { Text("Agregar al carrito") }
                        }
                    }
                    if (cartNotice != null) Text(cartNotice, color = MaterialTheme.colorScheme.primary)
                    if (cartError != null) Text(storeMessage(cartError), color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onCart, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver mi carrito")
                    }
                }
                HorizontalDivider()
                Text("Categoría: ${product.category}", style = MaterialTheme.typography.bodySmall)
                Text("Código de producto: ${product.remoteId ?: product.id}", style = MaterialTheme.typography.bodySmall)
                Text("Los pedidos no generan pagos ni envíos reales.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
