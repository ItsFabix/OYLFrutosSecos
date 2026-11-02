package com.example.oylfrutossecos.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.ui.components.OylBrand

@Composable
fun WelcomeScreen(onStartClick: () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visible, enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 4 }) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFFFF8EE)).safeDrawingPadding()) {
            val heroHeight = (maxHeight - 280.dp).coerceIn(320.dp, 480.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(heroHeight)) {
                    com.example.oylfrutossecos.ui.components.OylArtwork(Modifier.fillMaxSize(), bowl = true,
                        bowlClearance = 260.dp)
                    Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        OylBrand(Modifier.height(175.dp), decorated = false)
                        Text("Productos naturales\nmás cerca de ti", style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Surface(shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), color = Color(0xFFFFFCF6)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(Icons.Default.Storefront to "Catálogo de productos",
                            Icons.Default.ReceiptLong to "Tus pedidos",
                            Icons.Default.LocationOn to "Puntos de retiro",
                            Icons.Default.Eco to "Frutos secos y semillas").forEach { (icon, text) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Button(onClick = onStartClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Comenzar") }
                        OutlinedButton(onClick = onStartClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Ya tengo una cuenta") }
                    }
                }
            }
        }
    }
}
