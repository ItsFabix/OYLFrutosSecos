package com.example.oylfrutossecos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrderSuccessScreen(orderId: Long?, onOrders: () -> Unit, onHome: () -> Unit, order: OrderSummary? = null,
    serverOrderId: String? = null) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        OrderParcelIllustration()
        Text("Pedido registrado", style = MaterialTheme.typography.headlineSmall)
        Text("El servidor confirmó el registro de tu pedido.", style = MaterialTheme.typography.bodyMedium)
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SuccessLine("N.º de pedido", orderId?.let { "#$it" } ?: "Referencia no disponible")
                serverOrderId?.let { Text("Referencia REST: $it", style = MaterialTheme.typography.bodySmall) }
                order?.let {
                    SuccessLine("Fecha", SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("es", "CL")).format(Date(it.createdAt)))
                    SuccessLine("Total", priceInPesos(it.totalClp))
                    SuccessLine("Modalidad", it.delivery.displayName)
                }
                order?.let { OrderStatusBadge(it.status) }
            }
        }
        Button(onClick = onOrders, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Ver mis pedidos") }
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Volver al inicio") }
        Text("El pedido está registrado en Spring Boot y su copia se conserva en este dispositivo. El stock ya fue descontado. No se realizan pagos ni envíos reales.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SuccessLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.labelMedium)
    }
}
