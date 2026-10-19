package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.model.PickupPoint
import java.util.Locale

/** Referencias geográficas para probar la interfaz; NO son ubicaciones de la tienda. */
object DemoPickupPoints {
    val all: List<PickupPoint> = listOf(
        PickupPoint(
            id = "centro",
            title = "Punto Centro (demo)",
            reference = "Referencia: Plaza de Armas, Santiago",
            schedule = "Horario ficticio: Lun a vie, 10:00 a 18:00",
            latitude = -33.4372,
            longitude = -70.6506
        ),
        PickupPoint(
            id = "nunoa",
            title = "Punto Ñuñoa (demo)",
            reference = "Referencia: Plaza Ñuñoa, Santiago",
            schedule = "Horario ficticio: Lun a vie, 11:00 a 17:00",
            latitude = -33.4554,
            longitude = -70.5982
        )
    )

    private fun decimal(value: Double) = String.format(Locale.US, "%.5f", value)

    /** Mapa de OpenStreetMap incrustado (requiere Internet en el emulador). */
    fun embedUrl(point: PickupPoint): String {
        val lat = decimal(point.latitude)
        val lng = decimal(point.longitude)
        val left = decimal(point.longitude - 0.011)
        val right = decimal(point.longitude + 0.011)
        val bottom = decimal(point.latitude - 0.006)
        val top = decimal(point.latitude + 0.006)
        return "https://www.openstreetmap.org/export/embed.html?bbox=$left%2C$bottom%2C$right%2C$top&layer=mapnik&marker=$lat%2C$lng"
    }

    /** Enlace para abrir indicaciones mediante la app de mapas o un navegador. */
    fun directionsUrl(point: PickupPoint): String =
        "https://www.google.com/maps/dir/?api=1&destination=${decimal(point.latitude)}%2C${decimal(point.longitude)}"

    fun browserMapUrl(point: PickupPoint): String =
        "https://www.openstreetmap.org/?mlat=${decimal(point.latitude)}&mlon=${decimal(point.longitude)}#map=16/${decimal(point.latitude)}/${decimal(point.longitude)}"
}
