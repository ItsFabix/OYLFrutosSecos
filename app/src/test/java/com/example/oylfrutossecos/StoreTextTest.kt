package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.AppNotice
import com.example.oylfrutossecos.data.model.NoticeKind
import com.example.oylfrutossecos.ui.components.*
import org.junit.Assert.*
import org.junit.Test

class StoreTextTest {
    private fun notice(kind: NoticeKind, title: String, message: String) =
        AppNotice(1, 9, kind, title, message, "inventory", 0, null)

    @Test fun legacyErrorsKeepTheirMeaningWithoutTechnicalTerms() {
        assertEquals("No se pudo abrir tu sesión. Intenta nuevamente.",
            storeMessage("No se pudo abrir la sesión local. Intenta nuevamente."))
        assertEquals("No se pudo crear el aviso. Intenta nuevamente.",
            storeMessage("No se pudo crear el aviso de prueba."))
        assertEquals("Hay una solicitud anterior pendiente de revisión. El carrito permanece protegido para evitar pedidos duplicados.",
            storeMessage("Hay una solicitud remota anterior conservada. No se puede convertir en un pedido local."))
        assertEquals("Este pedido anterior es de solo consulta. Su estado no se puede cambiar desde aquí.",
            storeMessage("El estado de un pedido remoto se gestiona en Spring Boot."))
    }

    @Test fun unknownErrorsAndValidationDetailsAreNotHidden() {
        val original = "Stock insuficiente: solo quedan 3 bolsas."
        assertEquals(original, storeMessage(original))
        assertEquals("No se pudo guardar el movimiento. Intenta de nuevo.",
            storeMessage("No se pudo guardar el movimiento local. Intenta de nuevo."))
        assertEquals("No se pudo registrar el movimiento. Intenta nuevamente.",
            storeMessage("Error al registrar el movimiento local."))
    }

    @Test fun oldInventoryNoticesArePresentedWithoutChangingStoredValues() {
        val original = notice(NoticeKind.STOCK_LOW, "Stock bajo (demo)", "Almendras: quedan 2 bolsas (demo).")
        assertEquals("Stock bajo", noticeTitle(original))
        assertEquals("Almendras: quedan 2 bolsas.", noticeMessage(original))
        assertEquals("Stock bajo (demo)", original.title)
        assertEquals("Almendras: quedan 2 bolsas (demo).", original.message)
        assertEquals(9L, original.userId)
        assertNull(original.readAt)
    }

    @Test fun deviceNoticeDoesNotPretendToBeARemoteMessage() {
        val original = notice(NoticeKind.TEST, "Aviso de prueba OYL",
            "Las notificaciones locales funcionan. No es un mensaje enviado desde Internet.")
        assertEquals("Aviso de OYL", noticeTitle(original))
        assertEquals("Este aviso se creó en tu dispositivo.", noticeMessage(original))
    }

    @Test fun otherNotificationContentIsPreserved() {
        val original = notice(NoticeKind.ORDER_STATUS, "Pedido #18 preparado", "Puedes consultar los detalles del pedido.")
        assertEquals(original.title, noticeTitle(original))
        assertEquals(original.message, noticeMessage(original))
    }
}
