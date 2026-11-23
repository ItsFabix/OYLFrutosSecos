package com.example.oylfrutossecos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.oylfrutossecos.domain.WeatherConditions
import com.example.oylfrutossecos.ui.viewmodel.PickupWeatherState
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PickupWeatherCard(state: PickupWeatherState, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("pickup-weather"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider()
        Text("Clima · Open-Meteo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        when {
            state.loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Consultando el clima…", style = MaterialTheme.typography.bodySmall)
            }
            state.error != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Text("El mapa sigue disponible.", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onRetry) { Text("Reintentar clima") }
            }
            state.weather != null -> {
                val weather = state.weather
                Text("${String.format(Locale.forLanguageTag("es-CL"), "%.1f", weather.temperatureCelsius)} °C · ${WeatherConditions.description(weather.weatherCode)}",
                    style = MaterialTheme.typography.bodySmall)
                Text("Dato del ${weather.observedAt.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))} · hora local",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
