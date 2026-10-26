package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.ui.viewmodel.RemoteCatalogState

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
            Text("Conexión con el servidor", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Los productos y los pedidos " +
                    "de esta pantalla se consultan directamente en el servidor.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onRefresh,
                enabled = !state.loading && !state.creatingOrder
            ) { Text("Consultar servidor") }
        }
        if (state.loading) {
            item { CircularProgressIndicator() }
        } else if (state.error != null) {
            item {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                Text("Comprueba que el servidor esté disponible " +
                    "y vuelve a intentarlo.")
            }
        } else if (state.health != null) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Conexión establecida", style = MaterialTheme.typography.titleLarge)
                        Text("Servicio: ${state.health.service}")
                        Text("Estado: ${state.health.status}")
                        Text("Modo: ${state.health.mode}")
                    }
                }
                Text("${state.products.size} productos en el servidor",
                    style = MaterialTheme.typography.titleMedium)
            }
            if (isAdmin) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Pedido directo al servidor",
                                style = MaterialTheme.typography.titleLarge)
                            Text("Solicita 1 bolsa de almendras al servidor. " +
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
                                Text("Pedido creado: ${order.id}",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("Estado: ${order.status} · Total: ${order.totalClp} CLP")
                                Text("ID de solicitud: ${order.requestId}",
                                    style = MaterialTheme.typography.bodySmall)
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
                                Text(state.orderError, color = MaterialTheme.colorScheme.error)
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
                        Text("Precio: ${product.priceClp} CLP · Stock servidor: ${product.stock}")
                        Text("ID: ${product.id}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Text(
                    "El catálogo se consulta en el servidor. " +
                        "La administración de inventario es local; " +
                        "sus cambios no se sincronizan con el servidor.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
