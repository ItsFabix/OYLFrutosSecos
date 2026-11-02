package com.example.oylfrutossecos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val OylColors = lightColorScheme(
    primary = OylGreen, onPrimary = Color.White,
    primaryContainer = OylSoftGreen, onPrimaryContainer = Color(0xFF173C29),
    secondary = Color(0xFF6B7552), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF0DE), onSecondaryContainer = OylInk,
    tertiary = Color(0xFF946E42), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4E4CE), onTertiaryContainer = Color(0xFF49351E),
    background = Color(0xFFF8F9FB), onBackground = Color(0xFF182130),
    surface = Color(0xFFF8F9FB), onSurface = Color(0xFF182130),
    surfaceVariant = Color(0xFFEEECE1), onSurfaceVariant = Color(0xFF6B685E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAF3E8),
    surfaceContainer = Color(0xFFF3EDDF),
    surfaceContainerHigh = Color(0xFFECE5D7),
    surfaceContainerHighest = Color(0xFFE7E1D3),
    outline = Color(0xFF868B7E), outlineVariant = Color(0xFFDDDCCD),
    error = Color(0xFFAA3B30), onError = Color.White,
    errorContainer = Color(0xFFFCE9E3), onErrorContainer = Color(0xFF762B23)
)

@Composable
fun OYLFrutosSecosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OylColors,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(14.dp),
            extraLarge = RoundedCornerShape(20.dp)
        ),
        content = content
    )
}
