package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.local.PendingCheckout
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.ui.viewmodel.CartState
import org.junit.Assert.*
import org.junit.Test

class LocalCartStateTest {
    private val item = CartItem(Product(1, "Nueces", "Frutos secos", "Bolsa", 4000, 250, 3, "nut"), 2)

    @Test fun validLocalCartCanEditAndCheckout() {
        val state = CartState(items = listOf(item), loading = false)
        assertTrue(state.canEdit)
        assertTrue(state.canCheckout)
        assertEquals(8000L, state.total)
    }

    @Test fun emptyLoadingBusyOrInsufficientStockCannotCheckout() {
        val ready = CartState(items = listOf(item), loading = false)
        assertFalse(ready.copy(items = emptyList()).canCheckout)
        assertFalse(ready.copy(loading = true).canCheckout)
        assertFalse(ready.copy(busy = true).canCheckout)
        assertFalse(ready.copy(items = listOf(item.copy(quantity = 4))).canCheckout)
        assertFalse(ready.copy(busy = true).canEdit)
    }

    @Test fun legacyPendingNeverEnablesLocalCheckoutEvenAtZeroStock() {
        val pending = PendingCheckout("legacy", "{ unchanged }", DeliveryMethod.PICKUP, "1:2:4000")
        val state = CartState(items = listOf(item.copy(product = item.product.copy(stock = 0))),
            loading = false, pending = pending)
        assertFalse(state.canCheckout)
        assertFalse(state.canEdit)
        assertEquals(pending, state.pending)
    }
}
