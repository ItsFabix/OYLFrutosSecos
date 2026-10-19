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
            Text("Conexión API REST", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Paso 11C · Spring Boot + Retrofit. Los productos y los pedidos " +
                    "de esta pantalla vienen del servidor, no de SQLite.",
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
                Text("Comprueba que Spring Boot esté iniciado en tu PC y " +
                    "que http://localhost:8080/api/health responda.")
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
                Text("${state.products.size} productos recibidos por Retrofit",
                    style = MaterialTheme.typography.titleMedium)
            }
            if (isAdmin) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Pedido de prueba desde Android",
                                style = MaterialTheme.typography.titleLarge)
                            Text("Enviar 1 bolsa de almendras al servidor ($3.490). " +
                                "Esto DESCUENTA el stock real de la base de datos H2 de prueba. " +
                                "No utiliza el carrito ni genera un pedido SQLite.",
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
                                        else if (state.requestId == null) "Crear pedido remoto de prueba"
                                        else "Reintentar solicitud pendiente")
                                }
                            } else {
                                Text("Pedido creado: ${order.id}",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("Estado: ${order.status} · Total: $${order.totalClp} CLP")
                                Text("ID de solicitud: ${order.requestId}",
                                    style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(
                                    onClick = onCreateDemoOrder,
                                    enabled = !state.creatingOrder,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Reenviar el MISMO pedido (prueba sin duplicar)") }
                                OutlinedButton(
                                    onClick = onNewDemoOrder,
                                    enabled = !state.creatingOrder,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Preparar otro pedido de prueba") }
                            }
                            if (state.orderError != null) {
                                Text(state.orderError, color = MaterialTheme.colorScheme.error)
                            }
                            if (state.catalogWarning != null) {
                                Text(state.catalogWarning,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Si hubo un error de conexión, reintenta la solicitud " +
                                "pendiente: se conserva su requestId para evitar otra rebaja.",
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
                    "Fase 11C: pedidos remotos solo en esta pantalla de pruebas. " +
                        "El carrito, los pedidos de la app y el inventario local " +
                        "siguen utilizando SQLite; aún no hay sincronización.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
