package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.restError
import com.example.oylfrutossecos.domain.StorePresentation
import com.example.oylfrutossecos.ui.components.storeMessage
import org.junit.Assert.*
import org.junit.Test

class StorePresentationTest {
    @Test fun quantitiesUseCorrectSingularPluralAndChileanCharacters() {
        assertEquals("0 bolsas", StorePresentation.quantity(0, "bolsa", "bolsas"))
        assertEquals("1 bolsa", StorePresentation.quantity(1, "bolsa", "bolsas"))
        assertEquals("2 bolsas", StorePresentation.quantity(2, "bolsa", "bolsas"))
        assertEquals("1 producto", StorePresentation.quantity(1, "producto", "productos"))
    }

    @Test fun inventoryAuditUsesProfessionalShortReferenceWithoutChangingOriginalNote() {
        val note = "Pedido REST a9209d94-cc09-43fb-ad38-0fa9788eca72"
        assertEquals("Descuento por pedido · a9209d94", StorePresentation.inventoryNote(note))
        assertEquals("Pedido REST a9209d94-cc09-43fb-ad38-0fa9788eca72", note)
        assertEquals("Reposición en Ñuñoa", StorePresentation.inventoryNote("Reposición en Ñuñoa"))
        assertEquals("Stock inicial", StorePresentation.inventoryNote("Stock inicial"))
    }
    private fun notice(kind: NoticeKind, title: String, message: String) =
        AppNotice(17, 9, kind, title, message, "orders", 1234, 1500)

    @Test fun statusCodesHaveSpanishLabelsWithoutChangingTheContract() {
        val labels = mapOf("PENDING" to "Pendiente", "CONFIRMED" to "Confirmado", "PREPARED" to "Preparado",
            "DELIVERED" to "Entregado", "CANCELLED" to "Cancelado")
        labels.forEach { (raw, label) -> assertEquals(label, StorePresentation.statusLabel(raw)) }
        assertEquals("Pendiente", OrderStatus.PENDING.displayName)
        assertEquals("PENDING", OrderStatus.PENDING.name)
        assertEquals("Estado no disponible", StorePresentation.statusLabel("INTERNAL_STATUS"))
        assertEquals("Confirmado", StorePresentation.statusLabel("confirmed"))
    }

    @Test fun eventsDescribeOnlyTheActualStateAndPreparedDeliveryMethod() {
        val titles = mapOf("PENDING" to "Pedido recibido", "CONFIRMED" to "Pedido confirmado",
            "PREPARED" to "Pedido preparado", "DELIVERED" to "Pedido entregado", "CANCELLED" to "Pedido cancelado")
        titles.forEach { (raw, title) -> assertEquals(title, StorePresentation.eventTitle(raw)) }
        assertTrue(StorePresentation.eventMessage("PENDING").contains("pendiente de confirmación"))
        assertTrue(StorePresentation.eventMessage("CONFIRMED").contains("pronto comenzaremos"))
        assertTrue(StorePresentation.eventMessage("PREPARED", "PICKUP").contains("listo para retirar"))
        assertTrue(StorePresentation.eventMessage("PREPARED", "DELIVERY").contains("preparado para despacho"))
        assertTrue(StorePresentation.eventMessage("PREPARED").contains("entrega o retiro"))
        assertEquals("Tu pedido ha sido entregado. ¡Gracias por tu compra!", StorePresentation.eventMessage("DELIVERED"))
        assertEquals("Tu pedido ha sido cancelado.", StorePresentation.eventMessage("CANCELLED"))
        assertEquals("Actualización del pedido", StorePresentation.eventTitle("UNKNOWN"))
        assertEquals("Consulta los detalles para conocer el estado de tu pedido.", StorePresentation.eventMessage("UNKNOWN"))
    }

    @Test fun oldReceiptDoesNotFalselyConfirmPurchaseAndKeepsEveryStoredField() {
        val original = notice(NoticeKind.ORDER_CREATED, "Pedido REST #5 registrado", "El servidor confirmó tu pedido.")
        val snapshot = original.copy()
        assertEquals("Pedido #5 recibido", StorePresentation.noticeTitle(original))
        assertEquals(StorePresentation.eventMessage("PENDING"), StorePresentation.noticeMessage(original))
        assertEquals(snapshot, original)
        val legacy = original.copy(title = "Nuevo pedido #5", message = "Tu pedido está pendiente de confirmación.")
        assertEquals("Pedido #5 recibido", StorePresentation.noticeTitle(legacy))
        assertEquals(StorePresentation.eventMessage("PENDING"), StorePresentation.noticeMessage(legacy))
    }

    @Test fun historicalStatusNoticesKeepTheirHistoricalStateRatherThanLatestOrderState() {
        for ((label, raw) in listOf("Pendiente de validación" to "PENDING", "Confirmado" to "CONFIRMED",
                "Preparado" to "PREPARED", "Entregado" to "DELIVERED", "Cancelado" to "CANCELLED",
                "Pendiente" to "PENDING", "PENDING" to "PENDING", "CONFIRMED" to "CONFIRMED",
                "PREPARED" to "PREPARED", "DELIVERED" to "DELIVERED", "CANCELLED" to "CANCELLED")) {
            val old = notice(NoticeKind.ORDER_STATUS, "Pedido REST #5: $label", "El servidor actualizó el estado de tu pedido.")
            assertEquals("Pedido #5 ${StorePresentation.statusLabel(raw).lowercase()}", StorePresentation.noticeTitle(old))
            assertEquals(StorePresentation.eventMessage(raw), StorePresentation.noticeMessage(old))
            assertEquals(StorePresentation.eventMessage(raw), StorePresentation.noticeMessage(old.copy(message = "El estado de tu pedido cambió.")))
        }
        val unknown = notice(NoticeKind.ORDER_STATUS, "Pedido #5", "El estado de tu pedido cambió.")
        assertEquals("Pedido #5", StorePresentation.noticeTitle(unknown))
        assertEquals(StorePresentation.eventMessage(""), StorePresentation.noticeMessage(unknown))
    }

    @Test fun customAndNewNoticeContentRemainsIntactIncludingAccentsAndReadState() {
        for (kind in NoticeKind.entries) {
            val current = notice(kind, "Información de Ñuñoa", "Tu pedido está listo para retirar en Ñuñoa.")
            assertEquals(current.title, StorePresentation.noticeTitle(current))
            assertEquals(current.message, StorePresentation.noticeMessage(current))
            assertEquals(1500L, current.readAt); assertEquals(9L, current.userId)
        }
        val stock = notice(NoticeKind.STOCK_LOW, "Stock bajo (demo)", "Chía: quedan 1 bolsas en el servidor.")
        assertEquals("Stock bajo", StorePresentation.noticeTitle(stock))
        assertEquals("Chía: queda 1 bolsa.", StorePresentation.noticeMessage(stock))
    }

    @Test fun errorPresentationPreservesValidationAndHidesTechnicalDetailsAndSecrets() {
        val messages = mapOf(401 to "La clave administrativa no es válida. Configura nuevamente tu acceso.",
            403 to "La clave administrativa no es válida. Configura nuevamente tu acceso.",
            404 to "No encontramos la información solicitada. Actualiza e inténtalo nuevamente.",
            409 to "No pudimos aplicar el cambio. Actualiza los datos antes de reintentar.",
            500 to "Ocurrió un problema. Inténtalo más tarde.")
        messages.forEach { (code, message) -> assertEquals(message, restError(LogicFixtures.http(code))) }
        assertEquals("No pudimos establecer una conexión. Inténtalo nuevamente.", restError(java.io.IOException("SECRET")))
        assertEquals("No pudimos completar la operación. Inténtalo nuevamente.", restError(IllegalStateException("SECRET")))
        for (raw in listOf("HTTP 500 SECRET", "SQLite constraint failed", "Spring Boot SECRET", "Endpoint desconocido")) {
            assertEquals("No pudimos completar la operación. Inténtalo nuevamente.", storeMessage(raw))
        }
        assertTrue(storeMessage("Mostrando la última copia REST guardada").contains("última información guardada"))
        assertTrue(storeMessage("Hay un pedido REST pendiente").contains("Recuperar pedido pendiente"))
        assertTrue(storeMessage("No se pudo confirmar el movimiento REST").contains("Actualiza el inventario"))
        assertEquals("Stock insuficiente: solo quedan 3 bolsas.", storeMessage("Stock insuficiente: solo quedan 3 bolsas."))
    }

    @Test fun orderReferencesRespectExistingIdentityWithoutShowingFullRemoteUuid() {
        val remote = "a9209d94-cc09-43fb-ad38-0fa9788eca72"
        assertEquals("Pedido #11", StorePresentation.orderTitle(LogicFixtures.order.copy(remoteOrderId = remote)))
        assertEquals("Pedido #11", StorePresentation.orderTitle(LogicFixtures.order.copy(remoteOrderId = null)))
        assertEquals("Pedido · a9209d94", StorePresentation.orderTitle(LogicFixtures.order.copy(id = -1, remoteOrderId = remote)))
        assertEquals("Pedido · sin referencia", StorePresentation.orderTitle(LogicFixtures.order.copy(id = -1, remoteOrderId = null)))
    }
}
