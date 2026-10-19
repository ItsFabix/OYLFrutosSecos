package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OrderSuccessScreen(orderId: Long?, serverOrderId: String? = null,
    onOrders: () -> Unit, onHome: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✓", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text("Pedido registrado", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(if (orderId == null) "Pedido de demostración" else "Número en tu historial: #$orderId")
        if (serverOrderId != null) Text("ID servidor: $serverOrderId",
            style = MaterialTheme.typography.bodySmall)
        Text("Estado: Pendiente de validación")
        Spacer(Modifier.height(8.dp))
        Text("Pedido ficticio enviado al servidor Spring Boot y cacheado en SQLite. " +
            "No hay cobro real ni envío a una tienda.",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOrders, modifier = Modifier.fillMaxWidth()) { Text("Ver mis pedidos") }
        TextButton(onClick = onHome) { Text("Volver al inicio") }
    }
}
