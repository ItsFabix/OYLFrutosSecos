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
import com.example.oylfrutossecos.domain.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
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
    onAdjustInventory: (Long) -> Unit,
    scan: (((QrScanEvent) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    var lastEvent by remember { mutableStateOf<QrScanEvent?>(null) }
    var manualCode by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    val feedback = qrFeedback(lastEvent, state.products, state.loading || state.error != null)
    val product = state.products.firstOrNull { it.id == feedback?.productId }
    val receive = rememberUpdatedState<(QrScanEvent) -> Unit>({ event ->
        scanning = false
        lastEvent = event
        showResult = event != QrScanEvent.Cancelled
    })
    fun startScan() {
        scanning = true
        lastEvent = null
        showResult = false
        try {
            if (scan != null) scan { receive.value(it) }
            else {
                val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                GmsBarcodeScanning.getClient(context, options).startScan()
                    .addOnSuccessListener { receive.value(QrScanEvent.Read(it.rawValue)) }
                    .addOnCanceledListener { receive.value(QrScanEvent.Cancelled) }
                    .addOnFailureListener { failure ->
                        receive.value(if (failure is ApiException && failure.statusCode == CommonStatusCodes.CANCELED)
                            QrScanEvent.Cancelled else QrScanEvent.Failed)
                    }
            }
        } catch (_: Exception) { receive.value(QrScanEvent.Failed) }
    }

    if (showResult && feedback != null) AlertDialog(
        onDismissRequest = { showResult = false },
        icon = { Icon(if (feedback.kind == QrFeedbackKind.VALID) Icons.Default.CheckCircle else Icons.Default.Info,
            null, tint = if (feedback.kind == QrFeedbackKind.VALID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) },
        title = { Text(feedback.title) }, text = { Text(feedback.message) },
        confirmButton = { TextButton(onClick = {
            showResult = false
            when (feedback.kind) {
                QrFeedbackKind.VALID -> feedback.productId?.let(onOpenProduct)
                QrFeedbackKind.CATALOG_UNAVAILABLE -> { onRefresh(); showResult = true }
                else -> startScan()
            }
        }) { Text(when (feedback.kind) {
            QrFeedbackKind.VALID -> "Ver producto"
            QrFeedbackKind.CATALOG_UNAVAILABLE -> "Actualizar catálogo"
            else -> "Volver a escanear"
        }) } },
        dismissButton = { TextButton(onClick = { showResult = false }) { Text("Cerrar") } }
    )

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
                    onClick = ::startScan,
                    enabled = !scanning,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (scanning) "Escaneando…" else "Escanear con cámara") }
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
                    onClick = { receive.value(QrScanEvent.Read(manualCode.trim())) },
                    enabled = !scanning && manualCode.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Buscar producto") }
            }
        }

        if (feedback != null && !showResult) {
            HorizontalDivider()
            if (product == null) {
                InfoBanner(feedback.title, feedback.message, error = true)
                if (feedback.kind == QrFeedbackKind.CATALOG_UNAVAILABLE || feedback.kind == QrFeedbackKind.NOT_FOUND) {
                    OutlinedButton(onClick = onRefresh) { Text("Actualizar catálogo") }
                }
            } else {
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
        InfoBanner("Información de productos", "Busca productos con su código. El lector de cámara utiliza los servicios de Google.")
    }
}
