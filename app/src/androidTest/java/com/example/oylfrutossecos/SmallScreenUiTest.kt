package com.example.oylfrutossecos

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.ui.screens.LoginScreen
import com.example.oylfrutossecos.ui.screens.PickupMapScreen
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.example.oylfrutossecos.ui.screens.ProductDetailScreen
import com.example.oylfrutossecos.ui.theme.OYLFrutosSecosTheme
import com.example.oylfrutossecos.ui.viewmodel.LoginState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Before
import org.junit.Test

/** Pantallas de 320 dp con el tema real; no usa repositorios ni datos personales. */
class SmallScreenUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun useTheSameWindowInsetsAsMainActivity() {
        compose.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
    }

    private fun snapshot(name: String) {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "sqlite-design-qa")
        check(directory.exists() || directory.mkdirs())
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun loginButtonRemainsReachableWithKeyboardAtSmallWidth() {
        var state by mutableStateOf(LoginState())
        var logins = 0
        compose.setContent {
            OYLFrutosSecosTheme {
                Surface { Box(Modifier.width(320.dp)) {
                    LoginScreen(state, { state = state.copy(alias = it) },
                        { state = state.copy(password = it) }, { state = state.copy(role = it) },
                        { logins++ }, {}, {})
                } }
            }
        }
        compose.onNode(hasSetTextAction() and hasText("Alias")).performScrollTo().performClick()
            .performTextInput("cliente")
        compose.onNode(hasSetTextAction() and hasText("Contraseña")).performScrollTo().performClick()
            .performTextInput("Cliente123")
        compose.waitUntil(10000) {
            var keyboard = false
            compose.runOnIdle {
                keyboard = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
            keyboard
        }
        compose.onNodeWithText("Ingresar").performScrollTo().assertIsDisplayed().assertIsEnabled()
        snapshot("login-teclado-320dp")
        compose.onNodeWithText("Ingresar").performClick()
        compose.runOnIdle { assertEquals(1, logins) }
    }

    @Test fun productDetailsKeepPriceStockAndCartActionReachableAtSmallWidth() {
        val product = Product(1, "Almendras naturales", "Frutos secos",
            "Almendras seleccionadas en una bolsa práctica para compartir.", 3490, 250, 2, "nut")
        var added = 0
        compose.setContent {
            OYLFrutosSecosTheme {
                Surface { Box(Modifier.width(320.dp)) {
                    ProductDetailScreen(product, false, null, {}, {}, onAdd = { added += it })
                } }
            }
        }
        compose.onNodeWithText("Pocas unidades · 2 bolsas").performScrollTo().assertIsDisplayed()
        snapshot("producto-detalle-320dp")
        compose.onNodeWithText("Agregar al carrito").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, added) }
        compose.onNodeWithText("Ver mi carrito").assertIsEnabled()
    }

    @Test fun mapSearchRemainsVisibleWithKeyboardAndRestoresRealActionsAt320dp() {
        compose.setContent { OYLFrutosSecosTheme { Surface {
            Box(Modifier.width(320.dp).height(640.dp)) { PickupMapScreen {} }
        } } }
        val search = compose.onNode(hasSetTextAction() and hasText("Buscar puntos de retiro"))
        search.performClick().performTextInput("Centro")
        compose.waitUntil(10000) {
            var visible = false
            compose.runOnIdle { visible = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true }
            visible
        }
        search.assertIsDisplayed()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Cómo llegar").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Cómo llegar").assertDoesNotExist()
        snapshot("mapa-teclado-320dp")
        closeSoftKeyboard()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Cómo llegar").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Cómo llegar").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Centrar mapa").assertIsDisplayed().assertIsEnabled()
        snapshot("mapa-320dp")
    }
}
