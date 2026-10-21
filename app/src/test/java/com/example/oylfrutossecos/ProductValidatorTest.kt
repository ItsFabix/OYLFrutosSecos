package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.ProductDraft
import com.example.oylfrutossecos.domain.*
import org.junit.Assert.*
import org.junit.Test

class ProductValidatorTest {
    private val valid = ProductFormInput("Almendras", "Frutos secos", "Bolsa natural", "3490", "250", "0")

    @Test fun validProductAndZeroInitialStockHaveNoErrors() {
        assertTrue(ProductValidator.validate(valid, true).isEmpty())
        assertEquals(0, ProductValidator.toDraft(valid, true, 99, null).stock)
    }

    @Test fun blankProductReportsEachFieldIndependently() {
        val errors = ProductValidator.validate(ProductFormInput("", "", "", "", "", ""), true)
        assertEquals(ProductField.entries.toSet(), errors.keys)
    }

    @Test fun eachInvalidValueIsAssignedOnlyToItsOwnField() {
        val inputs = mapOf(
            ProductField.NAME to valid.copy(name = "ab"),
            ProductField.CATEGORY to valid.copy(category = "Categoría inexistente"),
            ProductField.DESCRIPTION to valid.copy(description = "abcd"),
            ProductField.PRICE to valid.copy(price = "0"),
            ProductField.WEIGHT to valid.copy(weight = "0"),
            ProductField.STOCK to valid.copy(initialStock = "-1"))
        inputs.forEach { (field, input) ->
            assertEquals(setOf(field), ProductValidator.validate(input, true).keys)
        }
    }

    @Test fun invalidNumbersAreNotSilentlyConvertedToPositiveIntegers() {
        for (raw in listOf("-1", "1.5", "1,5", "abc", "2147483648", "999999999999999999999")) {
            assertTrue(raw, ProductValidator.validate(valid.copy(price = raw), true).containsKey(ProductField.PRICE))
            assertTrue(raw, ProductValidator.validate(valid.copy(weight = raw), true).containsKey(ProductField.WEIGHT))
            assertTrue(raw, ProductValidator.validate(valid.copy(initialStock = raw), true).containsKey(ProductField.STOCK))
        }
    }

    @Test fun editingDoesNotValidateOrReplaceInventoryThroughInitialStock() {
        val input = valid.copy(initialStock = "invalid read-only value")
        assertTrue(ProductValidator.validate(input, false).isEmpty())
        assertEquals(17, ProductValidator.toDraft(input, false, 17, null).stock)
    }

    @Test fun draftAndFormUseTheSameRules() {
        val draft = ProductDraft("ab", "Frutos secos", "Bolsa natural", 3490, 250, 0)
        assertEquals(ProductValidator.validate(draft).values.first(), draft.validationError())
        assertNull(draft.copy(name = "Almendras").validationError())
    }

    @Test fun validDraftTrimsTextAndPreservesImageInstruction() {
        val draft = ProductValidator.toDraft(valid.copy(name = " Almendras ", price = " 3490 "), true, 0, "photo:new.jpg")
        assertEquals("Almendras", draft.name)
        assertEquals(3490, draft.priceClp)
        assertEquals("photo:new.jpg", draft.imageKey)
    }
}
