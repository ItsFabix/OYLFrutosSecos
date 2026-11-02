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
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.ui.screens.*
import com.example.oylfrutossecos.ui.theme.OYLFrutosSecosTheme
import com.example.oylfrutossecos.ui.viewmodel.CatalogState
import com.example.oylfrutossecos.ui.viewmodel.NotificationUiState
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Presentación con datos en memoria: nunca abre la base del usuario ni lanza la cámara. */
class StoreRedesignUiTest {
    @get:Rule val compose = createComposeRule()
    private val product = Product(1, "Almendras naturales", "Frutos secos", "Bolsa seleccionada", 3490, 250, 8, "nut")
    private val state = CatalogState(products = listOf(product), loading = false)

    @Test fun localNotificationSettingsRemainReachableAfterExpandingTheFooter() {
        var created = 0
        var permission = 0
        compose.setContent { OYLFrutosSecosTheme { Surface {
            Box(Modifier.width(320.dp)) {
                NotificationsScreen(NotificationUiState(loading = false), {}, {}, {}, {}, {},
                    { created++ }, false, { permission++ })
            }
        } } }
        compose.onNodeWithTag("notices-list").performScrollToNode(hasText("Avisos en el dispositivo"))
        compose.onNodeWithText("Avisos en el dispositivo").performClick()
        compose.onNodeWithTag("notices-list").performScrollToNode(hasText("Permitir notificaciones"))
        compose.onNodeWithText("Permitir notificaciones").performClick()
        compose.onNodeWithTag("notices-list").performScrollToNode(hasText("Crear aviso"))
        compose.onNodeWithText("Crear aviso").performClick()
        compose.runOnIdle { assertEquals(1, created); assertEquals(1, permission) }
    }

    private fun snapshot(name: String) {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "redesign-qa")
        check(directory.exists() || directory.mkdirs())
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun catalogCardOpensExistingProductAt320dp() {
        var opened: Long? = null
        compose.setContent { OYLFrutosSecosTheme { Surface {
            Box(Modifier.width(320.dp)) { CatalogScreen(state, {}, {}, {}, {}, { opened = it }) }
        } } }
        compose.onNode(hasSetTextAction() and hasText("Buscar")).assertIsDisplayed()
        snapshot("catalogo-encabezado")
        compose.onNodeWithTag("catalog-list").performScrollToNode(hasText("Almendras naturales"))
        compose.onNodeWithContentDescription("Ver producto Almendras naturales").assertIsDisplayed()
        snapshot("catalogo-producto")
        compose.onNodeWithContentDescription("Ver producto Almendras naturales").performClick()
        compose.runOnIdle { assertEquals(1L, opened) }
    }

    @Test fun adminHasDistinctCreationInventoryAndEditActions() {
        var created = 0
        var inventory = 0
        var edited: Long? = null
        compose.setContent { OYLFrutosSecosTheme { Surface {
            Box(Modifier.width(320.dp)) {
                ManageProductsScreen(state, { created++ }, { edited = it }, {}, { inventory++ })
            }
        } } }
        snapshot("gestion-encabezado")
        compose.onNodeWithText("Crear producto").assertIsDisplayed().performClick()
        compose.onNodeWithText("Inventario").performClick()
        compose.onNodeWithTag("manage-products-list").performScrollToNode(hasContentDescription("Editar Almendras naturales"))
        snapshot("gestion-producto")
        compose.onNodeWithContentDescription("Editar Almendras naturales").performClick()
        compose.runOnIdle { assertEquals(1, created); assertEquals(1, inventory); assertEquals(1L, edited) }
        compose.onNodeWithText("Ver producto").assertDoesNotExist()
    }

    @Test fun accountActionsRemainAvailableAndLogoutRemainsIndependent() {
        val user = User(7, "Cliente OYL", "cliente", "cliente@example.cl", "912345678", "Calle Sur 123", UserRole.CLIENT)
        var orders = 0; var notices = 0; var map = 0; var logout = 0
        compose.setContent { OYLFrutosSecosTheme { Surface {
            Box(Modifier.width(320.dp)) {
                AccountScreen(user, false, null, { logout++ }, { orders++ }, { map++ }, { notices++ }, 2)
            }
        } } }
        snapshot("cuenta-encabezado")
        for (label in listOf("Mis pedidos", "Notificaciones (2)", "Puntos de retiro", "Cerrar sesión")) {
            compose.onNodeWithTag("account-list").performScrollToNode(hasText(label))
            compose.onNodeWithText(label).performClick()
        }
        snapshot("cuenta-acciones")
        compose.runOnIdle { assertEquals(1, orders); assertEquals(1, notices); assertEquals(1, map); assertEquals(1, logout) }
        compose.onNodeWithText("Conexión con el servidor").assertDoesNotExist()
        compose.onNodeWithText("Créditos de fotografías").assertDoesNotExist()
    }

    @Test fun qrManualSearchKeepsValidationProductAndInventoryCallbacks() {
        var opened: Long? = null
        var adjusted: Long? = null
        compose.setContent { OYLFrutosSecosTheme { Surface {
            Box(Modifier.width(320.dp)) { QrScannerScreen(state, true, {}, { opened = it }, { adjusted = it }) }
        } } }
        compose.onNodeWithText("Escáner QR").assertIsDisplayed()
        snapshot("qr-encabezado")
        val field = compose.onNode(hasSetTextAction() and hasText("Código de producto"))
        field.performScrollTo().performTextInput("incorrecto")
        closeSoftKeyboard()
        compose.onNodeWithText("Buscar producto").performScrollTo().performClick()
        compose.onNodeWithText("Revisa el código").performScrollTo().assertIsDisplayed()
        field.performScrollTo().performTextReplacement("OYL:1")
        closeSoftKeyboard()
        compose.onNodeWithText("Buscar producto").performScrollTo().performClick()
        compose.onNodeWithText("Stock del catálogo: 8 bolsas").performScrollTo().assertIsDisplayed()
        snapshot("qr-resultado")
        compose.onNodeWithText("Ver detalle del producto").performScrollTo().performClick()
        compose.onNodeWithText("Registrar movimiento de inventario").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1L, opened); assertEquals(1L, adjusted) }
    }
}
