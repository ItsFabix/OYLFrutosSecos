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
    onBack: () -> Unit,
    onRefresh: () -> Unit
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
                "Paso 11A · Spring Boot + Retrofit. Los datos siguientes vienen " +
                    "del servidor de prueba, no del catálogo SQLite.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onRefresh, enabled = !state.loading) {
                Text("Consultar servidor")
            }
        }
        if (state.loading) {
            item { CircularProgressIndicator() }
        } else if (state.error != null) {
            item {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                Text("Comprueba que la terminal de Spring Boot diga Started y " +
                    "que http://localhost:8080/api/health funcione en tu PC.")
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
            items(state.products, key = { it.id }) { product ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(product.name, style = MaterialTheme.typography.titleMedium)
                        Text("${product.category} · ${product.weightGrams} g")
                        Text("Precio: ${product.priceClp} CLP · Stock servidor: ${product.stock}")
                        Text("ID: ${product.id}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Text(
                    "Etapa inicial de conectividad: el carrito, el inventario y los pedidos " +
                        "siguen usando SQLite. No hay sincronización ni descuento remoto todavía.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
