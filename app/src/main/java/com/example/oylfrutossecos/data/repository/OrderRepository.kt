package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.local.RemoteCheckoutCache
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlacedRemoteOrder(val localId: Long, val serverId: String)

/** Spring Boot es la fuente de pedidos y stock; SQLite conserva el historial local. */
class OrderRepository(
    private val database: OylDatabase,
    private val api: OylRestService
) {
    private val checkout = CheckoutCoordinator(RemoteCheckoutCache(database), api)

    suspend fun placeOrder(user: User, delivery: DeliveryMethod,
                           expectedItems: List<CartItem>): PlacedRemoteOrder =
        withContext(Dispatchers.IO) { checkout.placeOrder(user, delivery, expectedItems) }

    suspend fun getOrders(user: User): List<OrderSummary> = withContext(Dispatchers.IO) {
        database.readOrders(if (user.role == UserRole.ADMIN) null else user.id)
    }

    suspend fun advanceStatus(orderId: Long): Unit = withContext(Dispatchers.IO) {
        // Solo aplicable a solicitudes históricas locales. El servidor gestiona las nuevas.
        database.advanceDemoOrder(orderId)
    }
}
