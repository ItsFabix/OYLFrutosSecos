package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderRepository(private val database: OylDatabase) {
    suspend fun placeOrder(user: User, delivery: DeliveryMethod,
        expectedItems: List<CartItem>): Long = withContext(Dispatchers.IO) {
        database.placeDemoOrder(user, delivery, expectedItems)
    }

    suspend fun getOrders(user: User): List<OrderSummary> = withContext(Dispatchers.IO) {
        database.readOrders(if (user.role == UserRole.ADMIN) null else user.id)
    }

    suspend fun advanceStatus(orderId: Long): Unit = withContext(Dispatchers.IO) {
        database.advanceDemoOrder(orderId)
    }
}
