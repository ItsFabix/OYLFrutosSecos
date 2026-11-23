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

    @Test fun legacyRequestIsProtectedAndCanOpenRecoveryWithoutCartEditing() {
        var recovery = 0
        compose.setContent {
            OYLFrutosSecosTheme { CartScreen(state, { _, _ -> fail("Frozen cart edited") },
                {}, {}, { recovery++ }) }
        }
        compose.onNodeWithText("Solicitud anterior protegida").assertIsDisplayed()
        compose.onNodeWithText("Recuperar pedido pendiente").performClick()
        assertEquals(1, recovery)
        compose.onNodeWithContentDescription("Quitar").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("+").assertIsNotEnabled()
        compose.onNodeWithText("−").assertIsNotEnabled()
        compose.onNodeWithText("Continuar al pedido").assertDoesNotExist()
    }

    @Test fun pendingConfirmationRecoversPersistedDeliveryEvenWithoutCartItems() {
        var delivery: DeliveryMethod? = null
        val user = User(7, "Cliente", "cliente", "demo@example.cl", "911111111", "Santiago", UserRole.CLIENT)
        compose.setContent {
            OYLFrutosSecosTheme { OrderConfirmationScreen(user, state.copy(items = emptyList()),
                OrderUiState(loading = false), {}, { delivery = it }) }
        }
        compose.onNodeWithText("Resultado pendiente").assertIsDisplayed()
        compose.onNodeWithText("Registrar pedido").assertDoesNotExist()
        compose.onNodeWithText("Recuperar pedido pendiente").performClick()
        assertEquals(DeliveryMethod.DELIVERY, delivery)
        assertEquals(pending, state.pending)
    }
}
