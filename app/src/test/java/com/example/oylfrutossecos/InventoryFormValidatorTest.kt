package com.example.oylfrutossecos

import com.example.oylfrutossecos.domain.*
import org.junit.Assert.*
import org.junit.Test

class InventoryFormValidatorTest {
    private val valid = InventoryFormInput("ENTRY", "2", "Reposición")

    @Test fun validEntryAndExitUseExistingInventoryRules() {
        assertTrue(InventoryFormValidator.validate(valid, 5).isEmpty())
        assertEquals(7, InventoryFormValidator.projectedStock(valid, 5))
        assertTrue(InventoryFormValidator.validate(valid.copy(type = "EXIT"), 5).isEmpty())
        assertEquals(3, InventoryFormValidator.projectedStock(valid.copy(type = "EXIT"), 5))
    }

    @Test fun typeQuantityAndNoteHaveIndependentErrors() {
        assertEquals(InventoryField.entries.toSet(), InventoryFormValidator.validate(
            InventoryFormInput("INVALID", "", ""), 5).keys)
        assertEquals(setOf(InventoryField.TYPE), InventoryFormValidator.validate(valid.copy(type = "INVALID"), 5).keys)
    }

    @Test fun quantityRejectsZeroNegativeFractionsTextAndOverflow() {
        for (raw in listOf("0", "-2", "2.5", "abc", "100001", "2147483648", "99999999999999999999")) {
            assertEquals(raw, setOf(InventoryField.QUANTITY),
                InventoryFormValidator.validate(valid.copy(quantity = raw), 5).keys)
        }
    }

    @Test fun exitCannotExceedStockButCanUseTheLastBag() {
        val exit = valid.copy(type = "EXIT", quantity = "5")
        assertTrue(InventoryFormValidator.validate(exit, 5).isEmpty())
        assertEquals(0, InventoryFormValidator.projectedStock(exit, 5))
        assertEquals(setOf(InventoryField.QUANTITY), InventoryFormValidator.validate(exit.copy(quantity = "6"), 5).keys)
        assertNull(InventoryFormValidator.projectedStock(exit.copy(quantity = "6"), 5))
    }

    @Test fun noteHasBothLowerAndUpperLimits() {
        for (note in listOf("", "ab", "a".repeat(121)))
            assertEquals(setOf(InventoryField.NOTE), InventoryFormValidator.validate(valid.copy(note = note), 5).keys)
        assertTrue(InventoryFormValidator.validate(valid.copy(note = "a".repeat(120)), 5).isEmpty())
    }

    @Test fun stockOverflowStillUsesTheExistingDomainRule() {
        assertEquals(setOf(InventoryField.QUANTITY), InventoryFormValidator.validate(valid, Int.MAX_VALUE).keys)
    }
}
