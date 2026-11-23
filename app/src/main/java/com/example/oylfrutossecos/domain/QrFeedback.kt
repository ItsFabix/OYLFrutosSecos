package com.example.oylfrutossecos.domain

import com.example.oylfrutossecos.data.model.Product

sealed interface QrScanEvent {
    data class Read(val raw: String?) : QrScanEvent
    data object Cancelled : QrScanEvent
    data object Failed : QrScanEvent
}

enum class QrFeedbackKind { VALID, INCOMPATIBLE, NOT_FOUND, CATALOG_UNAVAILABLE, FAILED }
data class QrFeedback(val kind: QrFeedbackKind, val title: String, val message: String, val productId: Long? = null)

/** Never includes an unknown payload, service exception or URL in user-facing feedback. */
fun qrFeedback(event: QrScanEvent?, products: List<Product>, catalogUnavailable: Boolean): QrFeedback? = when (event) {
    null, QrScanEvent.Cancelled -> null
    QrScanEvent.Failed -> QrFeedback(QrFeedbackKind.FAILED, "No se pudo iniciar el escáner",
        "Comprueba la cámara y los servicios de Google Play. Puede ser necesaria una conexión para instalar el lector. Puedes reintentar.")
    is QrScanEvent.Read -> {
        val id = QrProductCode.parse(event.raw.orEmpty())
        val product = products.firstOrNull { (it.remoteId ?: it.id) == id }
        when {
            id == null -> QrFeedback(QrFeedbackKind.INCOMPATIBLE, "QR escaneado, pero no compatible",
                "Se leyó el código, pero su contenido no corresponde a un producto OYL. Usa un código como OYL:1.")
            catalogUnavailable -> QrFeedback(QrFeedbackKind.CATALOG_UNAVAILABLE, "Catálogo no disponible",
                "El código tiene un formato reconocido. Actualiza el catálogo para comprobar si el producto existe.")
            product == null -> QrFeedback(QrFeedbackKind.NOT_FOUND, "Producto no encontrado",
                "El código tiene un formato reconocido, pero no existe ese producto en el catálogo disponible.")
            else -> QrFeedback(QrFeedbackKind.VALID, "¡Producto OYL reconocido!",
                "Se encontró ${product.name}. Puedes consultar su detalle.", product.id)
        }
    }
}
