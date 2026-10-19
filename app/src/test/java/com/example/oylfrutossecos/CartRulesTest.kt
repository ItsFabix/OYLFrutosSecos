package com.example.oylfrutossecos

import com.example.oylfrutossecos.domain.CartRules
import org.junit.Assert.*
import org.junit.Test

class CartRulesTest {
    @Test fun rejectsInvalidQuantity() {
        assertFalse(CartRules.canSetQuantity(0, 8))
        assertFalse(CartRules.canSetQuantity(9, 8))
        assertFalse(CartRules.canSetQuantity(1, 0))
    }

    @Test fun permitsAvailableQuantity() {
        assertTrue(CartRules.canSetQuantity(1, 8))
        assertTrue(CartRules.canSetQuantity(8, 8))
    }

    @Test fun computesTotalInPesos() {
        assertEquals(9470L, CartRules.total(listOf(3490 to 2, 2490 to 1)))
    }
}
