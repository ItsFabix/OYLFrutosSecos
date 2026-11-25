package com.example.oylfrutossecos.domain

import com.example.oylfrutossecos.data.model.AppNotice
import com.example.oylfrutossecos.data.model.NoticeKind
import com.example.oylfrutossecos.data.model.OrderSummary
import java.util.Locale

/** Presentation only: never rewrites stored notices, status codes, identities or requests. */
object StorePresentation {
    fun quantity(count: Int, singular: String, plural: String): String =
        "$count ${if (count == 1) singular else plural}"

    fun inventoryNote(note: String): String {
        val order = Regex("^Pedido REST ([a-fA-F0-9-]{36})$").matchEntire(note)
        return if (order != null) "Descuento por pedido · ${order.groupValues[1].take(8)}" else note
    }

    fun statusLabel(status: String): String = when (status.uppercase(Locale.ROOT)) {
        "PENDING" -> "Pendiente"
        "CONFIRMED" -> "Confirmado"
        "PREPARED" -> "Preparado"
        "DELIVERED" -> "Entregado"
        "CANCELLED" -> "Cancelado"
        else -> "Estado no disponible"
    }

    fun eventTitle(status: String): String = when (status) {
        "PENDING" -> "Pedido recibido"
        "CONFIRMED" -> "Pedido confirmado"
        "PREPARED" -> "Pedido preparado"
        "DELIVERED" -> "Pedido entregado"
        "CANCELLED" -> "Pedido cancelado"
        else -> "Actualización del pedido"
    }

    fun eventMessage(status: String, delivery: String? = null): String = when (status) {
        "PENDING" -> "Hemos recibido tu pedido correctamente. Está pendiente de confirmación."
        "CONFIRMED" -> "Tu pedido ha sido confirmado y pronto comenzaremos a prepararlo."
        "PREPARED" -> when (delivery) {
            "PICKUP" -> "Tu pedido está listo para retirar. Revisa los detalles para coordinar el retiro."
            "DELIVERY" -> "Tu pedido está preparado para despacho."
            else -> "Tu pedido está preparado. Consulta los detalles de entrega o retiro."
        }
        "DELIVERED" -> "Tu pedido ha sido entregado. ¡Gracias por tu compra!"
        "CANCELLED" -> "Tu pedido ha sido cancelado."
        else -> "Consulta los detalles para conocer el estado de tu pedido."
    }

    // Positive IDs are SQLite order numbers; negative IDs are temporary administrative rows.
    // A short remote reference identifies those rows without pretending to be a local order number.
    fun orderTitle(order: OrderSummary): String = if (order.id > 0) "Pedido #${order.id}"
        else "Pedido · ${order.remoteOrderId?.take(8) ?: "sin referencia"}"

    private val legacyOrder = Regex("^(?:Nuevo )?Pedido(?: REST| remoto| local)? #(\\d+)(?: registrado|: (.+))?$", RegexOption.IGNORE_CASE)
    private fun legacyStatus(title: String): String? {
        val suffix = legacyOrder.matchEntire(title)?.groupValues?.get(2)?.lowercase(Locale.ROOT)
        return when (suffix) {
            "pendiente", "pendiente de validación", "pending" -> "PENDING"
            "confirmado", "confirmed" -> "CONFIRMED"
            "preparado", "prepared" -> "PREPARED"
            "entregado", "delivered" -> "DELIVERED"
            "cancelado", "cancelled" -> "CANCELLED"
            else -> null
        }
    }

    fun noticeTitle(notice: AppNotice): String = when (notice.kind) {
        NoticeKind.ORDER_CREATED -> legacyOrder.matchEntire(notice.title)?.let {
            "Pedido #${it.groupValues[1]} recibido"
        } ?: notice.title
        NoticeKind.ORDER_STATUS -> legacyStatus(notice.title)?.let {
            val number = legacyOrder.matchEntire(notice.title)!!.groupValues[1]
            "Pedido #$number ${statusLabel(it).lowercase(Locale.ROOT)}"
        } ?: notice.title
        NoticeKind.STOCK_LOW -> notice.title.removeSuffix(" (demo)")
        NoticeKind.TEST -> if (notice.title == "Aviso de prueba OYL") "Aviso de OYL" else notice.title
    }

    fun noticeMessage(notice: AppNotice): String = when (notice.kind) {
        NoticeKind.ORDER_CREATED -> if (notice.message in setOf("El servidor confirmó tu pedido.",
                "Tu pedido está pendiente de confirmación.")) eventMessage("PENDING") else notice.message
        NoticeKind.ORDER_STATUS -> if (notice.message in setOf("El servidor actualizó el estado de tu pedido.",
                "El estado de tu pedido cambió.")) eventMessage(legacyStatus(notice.title) ?: "") else notice.message
        NoticeKind.STOCK_LOW -> notice.message.replace(" bolsas (demo).", " bolsas.")
            .replace(" bolsas en el servidor.", " bolsas.").replace("quedan 1 bolsas", "queda 1 bolsa")
        NoticeKind.TEST -> if (notice.message == "Las notificaciones locales funcionan. No es un mensaje enviado desde Internet.")
            "Este aviso se creó en tu dispositivo." else notice.message
    }
}
