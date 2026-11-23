package com.example.oylfrutossecos.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

data class OpenMeteoResponse(val current: OpenMeteoCurrent?)

data class OpenMeteoCurrent(
    val time: String?,
    @SerializedName("temperature_2m") val temperature: Double?,
    @SerializedName("weather_code") val weatherCode: Int?
)

/** API externa pública: no comparte rutas, credenciales ni cliente con Spring Boot. */
interface OpenMeteoService {
    @GET("v1/forecast")
    suspend fun currentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}
