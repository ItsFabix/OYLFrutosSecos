package com.example.oylfrutossecos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit
) {
    val green = Color(0xFF285943)
    val cream = Color(0xFFFFF8EE)

    val visibleState = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cream)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(700)) +
                    slideInVertically(tween(700)) { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE1ECDD),
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = green,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Text(
                    text = "OYL",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = green
                )

                Text(
                    text = "Frutos Secos",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = green
                )

                Text(
                    text = "Lo natural, más cerca de ti",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF524A40)
                )

                Text(
                    text = "Explora frutos secos, semillas y " +
                            "deshidratados. Elige tus favoritos y " +
                            "realiza tu pedido desde el celular.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF625D54)
                )

                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = green,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Comenzar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}