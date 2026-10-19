package com.example.oylfrutossecos.data.model

/**
 * Puntos y horarios FICTICIOS para uso académico; no representan sucursales OYL reales.
 * Se reemplazarán por datos confirmados y/o API REST cuando existan.
 */
data class PickupPoint(
    val id: String,
    val title: String,
    val reference: String,
    val schedule: String,
    val latitude: Double,
    val longitude: Double
)
