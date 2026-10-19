package com.example.oylfrutossecos.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.R
import com.example.oylfrutossecos.data.model.Product
import java.text.NumberFormat
import java.util.Locale

fun priceInPesos(value: Int): String = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
    .apply { maximumFractionDigits = 0; minimumFractionDigits = 0 }.format(value)

@Composable
fun ProductImage(product: Product, modifier: Modifier = Modifier) {
    val resource = when (product.imageKey) {
        "fruit" -> R.drawable.oyl_fruit
        "seed" -> R.drawable.oyl_seed
        else -> R.drawable.oyl_nut
    }
    Box(modifier.clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Image(painterResource(resource), contentDescription = "Ilustración de ${product.category}",
            modifier = Modifier.fillMaxSize().padding(12.dp))
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            ProductImage(product, Modifier.size(84.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text("Bolsa de ${product.weightGrams} g", style = MaterialTheme.typography.bodySmall)
                Text(priceInPesos(product.priceClp), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium)
                Text(if (product.stock == 0) "Agotado" else "Stock: ${product.stock} bolsas",
                    color = if (product.stock == 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                Text("Ver detalle", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
