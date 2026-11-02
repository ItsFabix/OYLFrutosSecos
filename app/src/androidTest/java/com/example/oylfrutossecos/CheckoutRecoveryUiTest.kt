package com.example.oylfrutossecos

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.oylfrutossecos.data.local.PendingCheckout
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.ui.screens.CartScreen
import com.example.oylfrutossecos.ui.screens.OrderConfirmationScreen
import com.example.oylfrutossecos.ui.theme.OYLFrutosSecosTheme
import com.example.oylfrutossecos.ui.viewmodel.CartState
import com.example.oylfrutossecos.ui.viewmodel.OrderUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CheckoutRecoveryUiTest {
    @get:Rule val compose = createComposeRule()
    private val pending = PendingCheckout("saved-request", "{}", DeliveryMethod.DELIVERY, "1:1:3490")
    private val state = CartState(items = listOf(CartItem(
        Product(1, "Almendras", "Frutos secos", "Bolsa", 9000, 250, 0, "nut"), 1)),
        loading = false, pending = pending)

    @Test fun legacyRequestIsProtectedWithoutRecoveryOrCartEditing() {
        compose.setContent {
            OYLFrutosSecosTheme { CartScreen(state, { _, _ -> fail("Frozen cart edited") },
                {}, {}, { fail("REST request must not become a local order") }) }
        }
        compose.onNodeWithText("Solicitud anterior protegida").assertIsDisplayed()
        compose.onNodeWithText("Recuperar pedido pendiente").assertDoesNotExist()
        compose.onNodeWithContentDescription("Quitar").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("+").assertIsNotEnabled()
        compose.onNodeWithText("−").assertIsNotEnabled()
        compose.onNodeWithText("Continuar al pedido").assertDoesNotExist()
    }

    @Test fun pendingConfirmationCannotSubmitEvenWithoutCartItems() {
        val user = User(7, "Cliente", "cliente", "demo@example.cl", "911111111", "Santiago", UserRole.CLIENT)
        compose.setContent {
            OYLFrutosSecosTheme { OrderConfirmationScreen(user, state.copy(items = emptyList()),
                OrderUiState(), {}, { fail("Pending request must stay untouched") }) }
        }
        compose.onNodeWithText("Pedido no disponible").assertIsDisplayed()
        compose.onNodeWithText("Registrar pedido").assertDoesNotExist()
        compose.onNodeWithText("Recuperar pedido pendiente").assertDoesNotExist()
        assertEquals(pending, state.pending)
    }
}
