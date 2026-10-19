package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.domain.CatalogFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogFilterTest {
    private val products = listOf(
        Product(1, "Maní tostado", "Frutos secos", "", 1000, 250, 2, "nut"),
        Product(2, "Semillas de chía", "Semillas", "", 2000, 250, 0, "seed"),
        Product(3, "Pasas rubias", "Deshidratados", "", 1500, 250, 4, "fruit")
    )

    @Test fun searchIgnoresAccentsCaseAndSurroundingSpaces() {
        assertEquals(listOf(1L), CatalogFilter.apply(products, "  MANI  ", null).map { it.id })
        assertEquals(listOf(2L), CatalogFilter.apply(products, "chia", null).map { it.id })
    }

    @Test fun categoryAndSearchMustBothMatch() {
        assertTrue(CatalogFilter.apply(products, "mani", "Semillas").isEmpty())
        assertEquals(listOf(2L), CatalogFilter.apply(products, "", "Semillas").map { it.id })
    }

    @Test fun emptySearchIncludesOutOfStockProducts() {
        assertEquals(products, CatalogFilter.apply(products, "  ", null))
    }

    @Test fun allSearchWordsMustMatchRegardlessOfOrder() {
        assertEquals(listOf(1L), CatalogFilter.apply(products, "tostado mani", null).map { it.id })
        assertTrue(CatalogFilter.apply(products, "mani pasas", null).isEmpty())
    }
}
