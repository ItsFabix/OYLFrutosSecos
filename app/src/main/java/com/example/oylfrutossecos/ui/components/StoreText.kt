package com.example.oylfrutossecos.ui.components

import com.example.oylfrutossecos.data.model.AppNotice
import com.example.oylfrutossecos.domain.StorePresentation

fun quantityLabel(count: Int, singular: String, plural: String): String = StorePresentation.quantity(count, singular, plural)

// Presentación de mensajes heredados: no cambia los errores ni los avisos almacenados.
fun storeMessage(message: String): String = when (message) {
    "No se pudo abrir la sesión local. Intenta nuevamente." -> "No se pudo abrir tu sesión. Intenta nuevamente."
    "Error al registrar el movimiento local." -> "No se pudo registrar el movimiento. Intenta nuevamente."
    "No se pudo guardar el movimiento local. Intenta de nuevo." -> "No se pudo guardar el movimiento. Intenta de nuevo."
    "No se pudo crear el aviso de prueba." -> "No se pudo crear el aviso. Intenta nuevamente."
    "Hay una solicitud remota anterior conservada. Este carrito requiere revisión antes de crear pedidos locales.",
    "Hay una solicitud remota anterior conservada. No se puede convertir en un pedido local." ->
        "Hay una solicitud anterior pendiente de revisión. El carrito permanece protegido para evitar pedidos duplicados."
    "El estado de un pedido remoto se gestiona en Spring Boot." ->
        "Este pedido anterior es de solo consulta. Su estado no se puede cambiar desde aquí."
    else -> when {
        message.contains("pedido REST pendiente") -> "Hay un pedido pendiente. Usa Recuperar pedido pendiente antes de modificar el carrito."
        message.contains("última copia REST") -> "Mostrando la última información guardada. Los precios y las existencias pueden haber cambiado. Actualiza antes de comprar."
        message.contains("No se pudo confirmar el movimiento REST") -> "No pudimos confirmar el movimiento. Actualiza el inventario antes de reintentar."
        Regex("(?i)\\b(REST|API|endpoint|backend|Spring Boot|SQLite|Retrofit|microservicio|servidor|JSON|HTTP|UUID|Exception|constraint|SQLSTATE)\\b").containsMatchIn(message) ->
            "No pudimos completar la operación. Inténtalo nuevamente."
        else -> message
    }
}

fun noticeTitle(notice: AppNotice): String = StorePresentation.noticeTitle(notice)
fun noticeMessage(notice: AppNotice): String = StorePresentation.noticeMessage(notice)
