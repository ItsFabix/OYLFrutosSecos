package com.example.oylfrutossecos.ui.components

import com.example.oylfrutossecos.data.model.AppNotice
import com.example.oylfrutossecos.data.model.NoticeKind

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
    else -> message
}

fun noticeTitle(notice: AppNotice): String = when (notice.kind) {
    NoticeKind.STOCK_LOW -> notice.title.removeSuffix(" (demo)")
    NoticeKind.TEST -> if (notice.title == "Aviso de prueba OYL") "Aviso de OYL" else notice.title
    else -> notice.title
}

fun noticeMessage(notice: AppNotice): String = when (notice.kind) {
    NoticeKind.STOCK_LOW -> notice.message.replace(" bolsas (demo).", " bolsas.")
    NoticeKind.TEST -> if (notice.message == "Las notificaciones locales funcionan. No es un mensaje enviado desde Internet.")
        "Este aviso se creó en tu dispositivo." else notice.message
    else -> notice.message
}
