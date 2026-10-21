package com.example.oylfrutossecos.data.model

/**
 * Puntos y horarios FICTICIOS para uso académico; no representan sucursales OYL reales.
 */
data class PickupPoint(
    val id: String,
    val title: String,
    val reference: String,
    val schedule: String,
    val latitude: Double,
    val longitude: Double
)
