package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.*
import com.example.oylfrutossecos.data.repository.mergeOrderHistory
import com.example.oylfrutossecos.domain.CheckoutLines
import org.junit.Assert.*
import org.junit.Test

class OrderHistoryTest {
    private val local = OrderSummary(1, 7, "Cliente", 100, OrderStatus.PENDING, DeliveryMethod.PICKUP,
        "", "", 3000, listOf(OrderLine(1, "Semillas", 1, 3000)), "remote-1")
    private val remote = ApiOrderResponse("remote-1", "req", "cliente", "PREPARED", 3000,
        listOf(ApiOrderItem(1, "Semillas", 1, 3000)), createdAt = 100)

    @Test fun serverStatusWinsAndDuplicateRemoteRecordsHaveOneRow() {
        val result = mergeOrderHistory(listOf(local), listOf(remote, remote))
        assertEquals(1, result.size)
        assertEquals(1L, result.single().id)
        assertEquals(7L, result.single().userId)
        assertEquals(OrderStatus.PREPARED, result.single().status)
    }

    @Test fun legacyLocalAndServerOrdersWithSimilarFieldsRemainSeparate() {
        val result = mergeOrderHistory(listOf(local.copy(remoteOrderId = null)), listOf(remote))
        assertEquals(2, result.size)
        assertEquals(1, result.count { it.remoteOrderId == null })
        assertEquals(1, result.count { it.remoteOrderId != null })
        assertEquals(2, result.map { it.id }.distinct().size)
    }

    @Test fun adminSeesOrdersFromAnotherDeviceWithoutCachingThemUnderCurrentUser() {
        val result = mergeOrderHistory(emptyList(), listOf(remote))
        assertEquals("remote-1", result.single().remoteOrderId)
        assertEquals(0L, result.single().userId)
        assertTrue(result.single().id < 0)
    }

    @Test fun checkoutUsesRemoteIdentityWhenLocalIdCollides() {
        val product = Product(10, "Remoto", "Semillas", "Bolsa", 3000, 250, 5, "seed", remoteId = 9)
        assertEquals("9:2", CheckoutLines.fingerprint(listOf(CartItem(product, 2))))
    }
}
