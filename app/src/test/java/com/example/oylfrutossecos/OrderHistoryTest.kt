package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.*
import com.example.oylfrutossecos.data.repository.mergeOrderHistory
import org.junit.Assert.*
import org.junit.Test

class OrderHistoryTest {
    private fun local(id: Long, remote: String? = null) = OrderSummary(id, 7, "Cliente", 100,
        OrderStatus.PENDING, DeliveryMethod.PICKUP, "", "", 3490,
        listOf(OrderLine(1, "Almendras", 1, 3490)), remote)
    private fun remote(id: String, status: String = "CONFIRMED") = ApiOrderResponse(id, "request", "cliente",
        status, 6980, listOf(ApiOrderItem(1, "Almendras", 2, 3490)), "DELIVERY", "911", "Santiago")

    @Test fun serverReplacesCachedCopyWithoutDuplicatingLocalHistory() {
        val result = mergeOrderHistory(listOf(local(1), local(2, "server-1")),
            listOf(remote("server-1"), remote("server-1"), remote("server-2")))
        assertEquals(3, result.size)
        assertEquals(local(1), result.first())
        val refreshed = result.single { it.remoteOrderId == "server-1" }
        assertEquals(2L, refreshed.id)
        assertEquals(OrderStatus.CONFIRMED, refreshed.status)
        assertEquals(DeliveryMethod.DELIVERY, refreshed.delivery)
        assertEquals(2, refreshed.lines.single().quantity)
        assertEquals(6980L, refreshed.totalClp)
    }

    @Test fun clientRefreshUsesServerStatusForEveryTransition() {
        for (status in OrderStatus.values()) {
            val result = mergeOrderHistory(listOf(local(1, "server-1")), listOf(remote("server-1", status.name)))
            assertEquals(1, result.size)
            assertEquals(status, result.single().status)
        }
    }

    @Test fun emptyServerKeepsOnlyPurelyLocalOrders() {
        assertEquals(listOf(local(1)), mergeOrderHistory(listOf(local(1), local(2, "deleted")), emptyList()))
    }
}
