package com.example.oylfrutossecos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.domain.QrScanEvent
import com.example.oylfrutossecos.ui.screens.QrScannerScreen
import com.example.oylfrutossecos.ui.theme.OYLFrutosSecosTheme
import com.example.oylfrutossecos.ui.viewmodel.CatalogState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class QrFeedbackUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var opened: Long? = null
    private fun render(scan: ((QrScanEvent) -> Unit) -> Unit) {
        val product = Product(101, "Almendras", "Frutos secos", "", 2000, 250, 5, "almonds", remoteId = 8)
        compose.setContent { OYLFrutosSecosTheme {
            QrScannerScreen(CatalogState(products = listOf(product), loading = false), false, {}, { opened = it }, {}, scan)
        } }
    }
    private fun scan() { compose.onNodeWithText("Escanear con cámara").performClick() }

    @Test fun validQrShowsConfirmationAndOpensOnlyRecognizedProductOnExplicitTap() {
        render { it(QrScanEvent.Read("OYL:8")) }
        scan()
        compose.onNodeWithText("¡Producto OYL reconocido!").assertIsDisplayed()
        compose.runOnIdle { assertNull(opened) }
        compose.onNodeWithText("Ver producto").performClick()
        compose.runOnIdle { assertEquals(101L, opened) }
    }
    @Test fun incompatibleQrHasVisibleAlertAndCanScanAgainSuccessfully() {
        var attempts = 0
        render { it(QrScanEvent.Read(if (attempts++ == 0) "https://unknown.test/private-token" else "OYL:8")) }
        scan()
        compose.onNodeWithText("QR escaneado, pero no compatible").assertIsDisplayed()
        compose.onNodeWithText("private-token", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Volver a escanear").performClick()
        compose.onNodeWithText("¡Producto OYL reconocido!").assertIsDisplayed()
        compose.runOnIdle { assertNull(opened); assertEquals(2, attempts) }
    }
    @Test fun recognizedFormatMissingProductShowsSpecificDialog() {
        render { it(QrScanEvent.Read("OYL:999")) }
        scan()
        compose.onNodeWithText("Producto no encontrado").assertIsDisplayed()
        compose.onNodeWithText("QR escaneado, pero no compatible").assertDoesNotExist()
        compose.onNodeWithText("Cerrar").performClick()
        compose.onNodeWithText("Escanear con cámara").assertIsEnabled()
    }
    @Test fun voluntaryCancellationIsSilentAndAllowsRetry() {
        var attempts = 0
        render { attempts++; it(QrScanEvent.Cancelled) }
        scan(); scan()
        compose.onNodeWithText("Escanear con cámara").assertIsEnabled()
        compose.onNodeWithText("Cerrar").assertDoesNotExist()
        compose.onNodeWithText("No se pudo iniciar el escáner").assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, attempts); assertNull(opened) }
    }
    @Test fun serviceFailureShowsRetryAndCancellationClearsPreviousError() {
        var attempts = 0
        render { it(if (attempts++ == 0) QrScanEvent.Failed else QrScanEvent.Cancelled) }
        scan()
        compose.onNodeWithText("No se pudo iniciar el escáner").assertIsDisplayed()
        compose.onNodeWithText("Volver a escanear").performClick()
        compose.onNodeWithText("No se pudo iniciar el escáner").assertDoesNotExist()
        compose.onNodeWithText("Escanear con cámara").assertIsEnabled()
    }
}
