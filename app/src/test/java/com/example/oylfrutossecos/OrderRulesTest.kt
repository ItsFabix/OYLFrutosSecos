package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.OrderLine
import com.example.oylfrutossecos.data.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderRulesTest {
    @Test fun `pedido avanza por estados sin saltos`() {
        assertEquals(OrderStatus.CONFIRMED, OrderStatus.PENDING.next())
        assertEquals(OrderStatus.PREPARED, OrderStatus.CONFIRMED.next())
        assertEquals(OrderStatus.DELIVERED, OrderStatus.PREPARED.next())
        assertNull(OrderStatus.DELIVERED.next())
    }

    @Test fun `subtotal conserva precio historico`() {
        assertEquals(6980L, OrderLine(1, "Almendras", 2, 3490).subtotalClp)
    }
}
