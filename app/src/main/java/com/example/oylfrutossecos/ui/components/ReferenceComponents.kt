package com.example.oylfrutossecos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReferenceTitle(title: String, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(40.dp)) {
            onBack?.let { IconButton(onClick = it) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
fun OylFilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = label,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest))
}

@Composable
fun OrderParcelIllustration() {
    Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(100.dp)) {
            fun face(color: Color, vararg points: Pair<Float, Float>) {
                drawPath(Path().apply {
                    moveTo(points[0].first * size.width, points[0].second * size.height)
                    points.drop(1).forEach { lineTo(it.first * size.width, it.second * size.height) }
                    close()
                }, color)
            }
            face(Color(0xFFF2C47D), .08f to .26f, .50f to .08f, .92f to .26f, .50f to .45f)
            face(Color(0xFFE7AC59), .08f to .26f, .50f to .45f, .50f to .94f, .08f to .74f)
            face(Color(0xFFD39444), .50f to .45f, .92f to .26f, .92f to .74f, .50f to .94f)
            face(Color(0xFFFFDEA6), .30f to .16f, .72f to .35f, .72f to .62f, .61f to .67f, .61f to .40f, .19f to .21f)
        }
        Icon(Icons.Default.CheckCircle, null, Modifier.size(42.dp).align(Alignment.BottomEnd),
            tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun OylBrand(modifier: Modifier = Modifier, compact: Boolean = false, decorated: Boolean = true) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (decorated) OylArtwork(Modifier.fillMaxSize())
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Eco, null, Modifier.size(if (compact) 36.dp else 46.dp), tint = MaterialTheme.colorScheme.primary)
            Text("OYL", fontSize = if (compact) 58.sp else 72.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary, lineHeight = if (compact) 60.sp else 74.sp)
            Text("FRUTOS SECOS", fontSize = if (compact) 16.sp else 18.sp,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
