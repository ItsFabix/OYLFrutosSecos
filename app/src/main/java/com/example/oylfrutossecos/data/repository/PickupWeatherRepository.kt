package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.model.PickupPoint
import com.example.oylfrutossecos.data.model.PickupWeather
import com.example.oylfrutossecos.data.remote.OpenMeteoClient
import com.example.oylfrutossecos.data.remote.OpenMeteoService
import java.time.LocalDateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Caché pública en memoria; no lee ni escribe SQLite, carrito o configuración administrativa. */
class PickupWeatherRepository(
    private val api: OpenMeteoService,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    private data class Coordinates(val latitude: Double, val longitude: Double)
    private data class Cached(val value: PickupWeather, val fetchedAt: Long)
    private val cache = mutableMapOf<Coordinates, Cached>()
    private val mutex = Mutex()

    suspend fun current(point: PickupPoint): PickupWeather = mutex.withLock {
        val key = Coordinates(point.latitude, point.longitude)
        cache[key]?.takeIf { nowMillis() - it.fetchedAt in 0 until CACHE_MILLIS }?.let { return@withLock it.value }
        val current = api.currentWeather(point.latitude, point.longitude).current
            ?: error("Respuesta meteorológica incompleta")
        val temperature = current.temperature?.takeIf { it.isFinite() }
            ?: error("Temperatura no disponible")
        val code = current.weatherCode ?: error("Condición meteorológica no disponible")
        val time = LocalDateTime.parse(current.time ?: error("Hora meteorológica no disponible"))
        val weather = PickupWeather(temperature, code, time)
        cache[key] = Cached(weather, nowMillis())
        weather
    }

    companion object {
        const val CACHE_MILLIS = 10 * 60 * 1000L
        // Conserva las dos consultas públicas al salir/volver al mapa, sin persistir datos.
        val shared: PickupWeatherRepository by lazy { PickupWeatherRepository(OpenMeteoClient.service) }
    }
}
