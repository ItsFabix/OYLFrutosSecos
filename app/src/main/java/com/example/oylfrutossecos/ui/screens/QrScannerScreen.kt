package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.domain.QrProductCode
import com.example.oylfrutossecos.ui.components.priceInPesos
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Escáner QR de productos", style = MaterialTheme.typography.headlineMedium)
        Text("Escanea un código de OYL Frutos Secos para consultar su ficha y sus existencias.")
        Button(
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
                            notice = "No se pudo abrir el lector. Revisa Google Play services y la conexión de Internet."
                        }
                } catch (_: Exception) {
                    notice = "No se pudo iniciar el lector QR en este dispositivo."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Escanear código QR con cámara") }

        Text("Buscar por código", style = MaterialTheme.typography.titleMedium)
        Text("Ingresa el código del producto, por ejemplo OYL:1.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = manualCode,
            onValueChange = { manualCode = it.take(120) },
            label = { Text("Código de producto") },
            placeholder = { Text("OYL:1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = { lastCode = manualCode.trim(); notice = null },
            enabled = manualCode.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Buscar producto") }

        if (notice != null) Text(notice.orEmpty(), color = MaterialTheme.colorScheme.primary)
        if (lastCode.isNotBlank()) {
            HorizontalDivider()
            Text("Código: ${lastCode.take(100)}", style = MaterialTheme.typography.bodySmall)
            when {
                productId == null -> Text("Código QR no válido para OYL. Usa OYL:ID, por ejemplo OYL:1.",
                    color = MaterialTheme.colorScheme.error)
                state.loading -> CircularProgressIndicator()
                state.error != null -> {
                    Text("No se pudo consultar el catálogo. ${state.error}", color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRefresh) { Text("Reintentar") }
                }
                product == null -> {
                    Text("No existe un producto con ese código en el catálogo local.",
                        color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRefresh) { Text("Actualizar catálogo") }
                }
                else -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("¡Producto encontrado!", color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium)
                            Text(product.name, style = MaterialTheme.typography.titleLarge)
                            Text("${product.category} · ${product.weightGrams} g")
                            Text("Precio: ${priceInPesos(product.priceClp)}")
                            Text("Stock del catálogo: ${product.stock} bolsas")
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
        Text("Consulta el catálogo guardado en este dispositivo. Los ajustes de inventario son locales.",
            style = MaterialTheme.typography.bodySmall)
    }
}
