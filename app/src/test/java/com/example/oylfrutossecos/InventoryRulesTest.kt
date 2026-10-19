package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.MovementType
import com.example.oylfrutossecos.domain.InventoryRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InventoryRulesTest {
    @Test fun entradaIncrementaStock() {
        assertEquals(15, InventoryRules.newStock(10, MovementType.ENTRY, 5))
    }
    @Test fun salidaDescuentaSinNegativos() {
        assertEquals(2, InventoryRules.newStock(5, MovementType.EXIT, 3))
    }
    @Test(expected = IllegalArgumentException::class)
    fun salidaNoPuedeExcederStock() {
        InventoryRules.newStock(2, MovementType.EXIT, 3)
    }
    @Test(expected = IllegalArgumentException::class)
    fun rechazaCantidadCero() {
        InventoryRules.newStock(2, MovementType.ENTRY, 0)
    }
    @Test fun validaMotivo() {
        assertNotNull(InventoryRules.validateNote("a"))
        assertNull(InventoryRules.validateNote("Reposición semanal"))
    }
}
