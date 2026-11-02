package com.example.oylfrutossecos.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.R
import com.example.oylfrutossecos.data.model.Product
import java.text.NumberFormat
import java.util.Locale

fun priceInPesos(value: Int): String = priceInPesos(value.toLong())

fun priceInPesos(value: Long): String = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
    .apply { maximumFractionDigits = 0; minimumFractionDigits = 0 }.format(value)

@Composable
fun ProductImage(product: Product, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(product.imageKey) { ProductPhotoStore.bitmap(context, product.imageKey) }
    val resource = when (product.imageKey) {
        "fruit" -> R.drawable.oyl_fruit
        "seed" -> R.drawable.oyl_seed
        else -> R.drawable.oyl_nut
    }
    Box(modifier.clip(RoundedCornerShape(7.dp))
        .background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = "Foto de ${product.name}",
                modifier = Modifier.fillMaxSize().testTag("product-photo-${product.id}"), contentScale = ContentScale.Crop)
        } else {
            Image(painterResource(resource), contentDescription = "Ilustración de ${product.name}",
                modifier = Modifier.fillMaxSize(.65f).testTag("product-placeholder-${product.id}"), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(7.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        ProductImage(product, Modifier.fillMaxWidth().height(86.dp))
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(product.name, style = MaterialTheme.typography.labelMedium, maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.heightIn(min = 32.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(priceInPesos(product.priceClp), style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("${product.weightGrams} g", style = MaterialTheme.typography.labelSmall)
                    Text(when {
                        product.stock == 0 -> "Agotado"
                        product.stock <= com.example.oylfrutossecos.domain.InventoryRules.LOW_STOCK_LIMIT -> "Stock bajo: ${product.stock}"
                        else -> "Stock: ${product.stock}"
                    }, style = MaterialTheme.typography.labelSmall,
                        color = if (product.stock == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
                    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Ver producto ${product.name}",
                            tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(5.dp).size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AdminProductCard(product: Product, onEdit: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            ProductImage(product, Modifier.size(60.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleSmall)
                Text(priceInPesos(product.priceClp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("${product.weightGrams} g · Stock: ${product.stock}", style = MaterialTheme.typography.labelSmall,
                    color = if (product.stock == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) {
                Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Editar ${product.name}",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
