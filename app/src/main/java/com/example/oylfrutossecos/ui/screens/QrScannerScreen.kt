package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.domain.QrProductCode
import com.example.oylfrutossecos.ui.components.*
import com.example.oylfrutossecos.ui.viewmodel.CatalogState
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

// Usa Google Play services para el escaneo; no requiere permiso CAMERA en la aplicación.
@Composable
fun QrScannerScreen(
    state: CatalogState,
    isAdmin: Boolean,
    onRefresh: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onAdjustInventory: (Long) -> Unit
) {
    val context = LocalContext.current
    var lastCode by remember { mutableStateOf("") }
    var manualCode by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val productId = QrProductCode.parse(lastCode)
    val product = state.products.firstOrNull { it.id == productId }

    Column(
        modifier = Modifier.fillMaxSize().testTag("qr-screen").verticalScroll(rememberScrollState()).imePadding().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CompactHeader("Escáner QR", "Escanea el código de un producto")
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF252A27))) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.QrCodeScanner, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    Column(Modifier.weight(1f)) {
                        Text("Lector de cámara", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
                        Text("Apunta al QR de OYL.", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                FilledTonalButton(
                    onClick = {
                        notice = null
                        try {
                            val options = GmsBarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                            GmsBarcodeScanning.getClient(context, options).startScan()
                                .addOnSuccessListener { barcode ->
                                    lastCode = barcode.rawValue.orEmpty().trim()
                                    notice = if (lastCode.isBlank()) "El QR no contenía información." else "Código QR leído."
                                }
                                .addOnCanceledListener { notice = "Escaneo cancelado." }
                                .addOnFailureListener {
                                    notice = "No se pudo abrir el lector. Comprueba que los servicios de Google estén actualizados y tengas conexión a Internet."
                                }
                        } catch (_: Exception) {
                            notice = "No se pudo iniciar el lector QR en este dispositivo."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Escanear con cámara") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Buscar por código", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it.take(120) },
                    label = { Text("Código de producto") }, shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Default.Tag, null) },
                    placeholder = { Text("OYL:1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { lastCode = manualCode.trim(); notice = null },
                    enabled = manualCode.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Buscar producto") }
            }
        }

        if (notice != null) InfoBanner("Estado del escáner", notice.orEmpty())
        if (lastCode.isNotBlank()) {
            HorizontalDivider()
            Text("Código: ${lastCode.take(100)}", style = MaterialTheme.typography.bodySmall)
            when {
                productId == null -> InfoBanner("Revisa el código", "Código QR no válido para OYL. Usa OYL:ID, por ejemplo OYL:1.", error = true)
                state.loading -> CircularProgressIndicator()
                state.error != null -> {
                    InfoBanner("Catálogo no disponible", "No se pudo consultar el catálogo. ${state.error}", error = true)
                    OutlinedButton(onClick = onRefresh) { Text("Reintentar") }
                }
                product == null -> {
                    InfoBanner("Producto no encontrado", "No existe un producto con ese código en el catálogo.", error = true)
                    OutlinedButton(onClick = onRefresh) { Text("Actualizar catálogo") }
                }
                else -> {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("¡Producto encontrado!", color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium)
                            ProductImage(product, Modifier.fillMaxWidth().height(96.dp))
                            Text(product.name, style = MaterialTheme.typography.titleLarge)
                            Text("${product.category} · ${product.weightGrams} g")
                            Text("Precio: ${priceInPesos(product.priceClp)}")
                            Text("Stock del catálogo: ${product.stock} bolsas")
                            StockBadge(product.stock)
                            Button(onClick = { onOpenProduct(product.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Ver detalle del producto")
                            }
                            if (isAdmin) {
                                OutlinedButton(onClick = { onAdjustInventory(product.id) },
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text("Registrar movimiento de inventario")
                                }
                            }
                        }
                    }
                }
            }
        }
        InfoBanner("Información de productos", "Busca productos con su código. El lector de cámara utiliza los servicios de Google.")
    }
}
