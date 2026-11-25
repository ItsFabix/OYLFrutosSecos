package com.example.oylfrutossecos

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.ui.screens.*
import com.example.oylfrutossecos.ui.theme.OYLFrutosSecosTheme
import com.example.oylfrutossecos.ui.viewmodel.NotificationUiState
import com.example.oylfrutossecos.ui.viewmodel.InventoryState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/** In-memory UI fixtures only: no singleton, real purchases or user history writes. */
class StorePresentationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun inventoryAuditShowsProfessionalReferenceInsteadOfFullTechnicalNote() {
        val note = "Pedido REST a9209d94-cc09-43fb-ad38-0fa9788eca72"
        val movement = InventoryMovement(1, 1, "Almendras", MovementType.EXIT, 1, 5, 4, note, 1234)
        compose.setContent { OYLFrutosSecosTheme { Surface {
            InventoryScreen(InventoryState(movements = listOf(movement), loading = false), {}, {}, {})
        } } }
        compose.onNodeWithTag("inventory-list").performScrollToNode(hasText("Motivo: Descuento por pedido · a9209d94"))
        compose.onNodeWithText("Motivo: Descuento por pedido · a9209d94").assertIsDisplayed()
        compose.onNodeWithText(note, substring = true).assertDoesNotExist()
        compose.runOnIdle { assertEquals(note, movement.note) }
    }

    @Test fun legacyNoticeShowsActualTranslatedStateWithoutTechnicalTextAt320dp() {
        val old = AppNotice(1, 9, NoticeKind.ORDER_STATUS, "Pedido REST #5: Entregado",
            "El servidor actualizó el estado de tu pedido.", "orders", 1234, null)
        var opened: Long? = null
        compose.setContent { OYLFrutosSecosTheme { Surface { Box(Modifier.width(320.dp)) {
            NotificationsScreen(NotificationUiState(items = listOf(old), loading = false), {}, {}, {}, {},
                { opened = it.id }, {}, true, {})
        } } } }
        compose.onNodeWithText("● Pedido #5 entregado").assertIsDisplayed().performClick()
        compose.onNodeWithText("Tu pedido ha sido entregado. ¡Gracias por tu compra!").assertIsDisplayed()
        compose.onNodeWithText(old.title).assertDoesNotExist()
        compose.onNodeWithText(old.message).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1L, opened); assertEquals("Pedido REST #5: Entregado", old.title) }
        val output = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "final-qa")
        check(output.exists() || output.mkdirs())
        File(output, "notificaciones.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun customerPreferencesDoNotExposeDiagnosticNoticeGenerator() {
        compose.setContent { OYLFrutosSecosTheme { Surface {
            NotificationsScreen(NotificationUiState(loading = false), {}, {}, {}, {}, {},
                { throw AssertionError("Customer cannot generate test data") }, true, {})
        } } }
        compose.onNodeWithTag("notices-list").performScrollToNode(hasText("Preferencias de notificaciones"))
        compose.onNodeWithText("Preferencias de notificaciones").performClick()
        compose.onNodeWithTag("notices-list").performScrollToNode(hasText("Las notificaciones están permitidas en este dispositivo."))
        compose.onNodeWithText("Crear aviso").assertDoesNotExist()
        compose.onNodeWithText("Comprobar notificaciones").assertDoesNotExist()
    }

    @Test fun receiptUsesOrderNumberAndDoesNotClaimConfirmationForPendingOrder() {
        val remote = "a9209d94-cc09-43fb-ad38-0fa9788eca72"
        val order = OrderSummary(5, 9, "Cliente", 1234, OrderStatus.PENDING, DeliveryMethod.PICKUP,
            "912345678", "Retiro", 3490, emptyList(), remote)
        compose.setContent { OYLFrutosSecosTheme { Surface { Box(Modifier.width(320.dp)) {
            OrderSuccessScreen(5, {}, {}, order, remote)
        } } } }
        compose.onNodeWithText("Hemos recibido tu pedido correctamente.").assertIsDisplayed()
        compose.onNodeWithText("Estado: Pendiente").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("#5").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(remote, substring = true).assertDoesNotExist()
        compose.onNodeWithText("Estado: Confirmado").assertDoesNotExist()
    }
}
