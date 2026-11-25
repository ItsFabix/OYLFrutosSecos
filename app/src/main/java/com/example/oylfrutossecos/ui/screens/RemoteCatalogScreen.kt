package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.ui.viewmodel.RemoteCatalogState
import com.example.oylfrutossecos.ui.components.priceInPesos
import com.example.oylfrutossecos.ui.components.storeMessage
import com.example.oylfrutossecos.domain.StorePresentation

@Composable
fun RemoteCatalogScreen(
    state: RemoteCatalogState,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCreateDemoOrder: () -> Unit,
    onNewDemoOrder: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("Volver") }
            Text("Diagnóstico de conexión", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Herramienta de evaluación académica. Las solicitudes de esta pantalla están separadas del carrito y del historial de la tienda.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onRefresh,
                enabled = !state.loading && !state.creatingOrder
            ) { Text("Actualizar catálogo") }
        }
        if (state.loading) {
            item { CircularProgressIndicator() }
        } else if (state.error != null) {
            item {
                Text(storeMessage(state.error), color = MaterialTheme.colorScheme.error)
                Text("Revisa tu conexión y vuelve a intentarlo.")
            }
        } else if (state.health != null) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Conexión establecida", style = MaterialTheme.typography.titleLarge)
                        Text("Catálogo disponible")
                    }
                }
                Text("${state.products.size} productos disponibles",
                    style = MaterialTheme.typography.titleMedium)
            }
            if (isAdmin) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Pedido de comprobación",
                                style = MaterialTheme.typography.titleLarge)
                            Text("Solicita 1 bolsa de almendras. " +
                                "Se descontará una unidad de su stock. " +
                                "No usa el carrito ni se guarda en el historial de la app. No genera pagos ni envíos.",
                                style = MaterialTheme.typography.bodyMedium)
                            val order = state.order
                            if (order == null) {
                                Button(
                                    onClick = onCreateDemoOrder,
                                    enabled = !state.creatingOrder &&
                                        state.products.any { it.id == 1L && it.stock > 0 },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (state.creatingOrder) "Enviando..."
                                        else if (state.requestId == null) "Registrar pedido sin pago"
                                        else "Reintentar solicitud pendiente")
                                }
                            } else {
                                Text("Pedido registrado · ${order.id.take(8)}",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("Estado: ${StorePresentation.statusLabel(order.status)} · Total: ${priceInPesos(order.totalClp)}")
                                OutlinedButton(
                                    onClick = onCreateDemoOrder,
                                    enabled = !state.creatingOrder,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Reenviar solicitud sin duplicar") }
                                OutlinedButton(
                                    onClick = onNewDemoOrder,
                                    enabled = !state.creatingOrder,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Preparar otro pedido") }
                            }
                            if (state.orderError != null) {
                                Text(storeMessage(state.orderError), color = MaterialTheme.colorScheme.error)
                            }
                            if (state.catalogWarning != null) {
                                Text(state.catalogWarning,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Si hubo un error de conexión, reintenta la solicitud " +
                                "pendiente: no se descontará stock dos veces.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            items(state.products, key = { it.id }) { product ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(product.name, style = MaterialTheme.typography.titleMedium)
                        Text("${product.category} · ${product.weightGrams} g")
                        Text("Precio: ${priceInPesos(product.priceClp)} · Existencias: ${product.stock}")
                    }
                }
            }
            item {
                Text(
                    "Los pedidos de comprobación descuentan existencias. No realizan cobros ni despachos reales.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
