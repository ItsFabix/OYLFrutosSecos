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

    suspend fun getOrders(user: User, adminKey: String = ""): OrderHistory = withContext(Dispatchers.IO) {
        val local = database.readOrders(if (user.role == UserRole.ADMIN) null else user.id)
        try {
            val remote = if (user.role == UserRole.ADMIN) {
                if (adminKey.isBlank()) return@withContext OrderHistory(local,
                    "Introduce la clave administrativa para consultar los pedidos REST.", false)
                api.adminOrders(adminKey)
            } else {
                local.mapNotNull { it.remoteOrderId }.distinct().map { api.getOrder(it) }
            }
            OrderHistory(mergeOrderHistory(local, remote), remoteFresh = true)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            OrderHistory(local, orderError(e) + " El historial local sigue disponible; los pedidos REST no están actualizados.", false)
        }
    }

    suspend fun advanceStatus(order: OrderSummary, adminKey: String): Unit = withContext(Dispatchers.IO) {
        if (order.remoteOrderId == null) database.advanceDemoOrder(order.id)
        else {
            check(adminKey.isNotBlank()) { "Introduce la clave administrativa." }
            api.advanceOrder(order.remoteOrderId, adminKey)
            Unit
        }
    }
}

data class OrderHistory(val orders: List<OrderSummary>, val error: String? = null,
                        val remoteFresh: Boolean = false)

/** Una fila por ID remoto; la copia SQLite nunca reemplaza el estado del servidor. */
internal fun mergeOrderHistory(local: List<OrderSummary>, remote: List<ApiOrderResponse>): List<OrderSummary> {
    val cached = local.mapNotNull { row -> row.remoteOrderId?.let { it to row } }.toMap()
    val server = remote.distinctBy { it.id }.mapIndexed { index, row ->
        val previous = cached[row.id]
        OrderSummary(previous?.id ?: -(index + 1L), previous?.userId ?: 0L,
            row.customerAlias, previous?.createdAt ?: 0L, OrderStatus.valueOf(row.status),
            DeliveryMethod.valueOf(row.delivery), row.contactPhone, row.contactAddress,
            row.totalClp, row.items.map { OrderLine(it.productId, it.productName, it.quantity, it.unitPriceClp) }, row.id)
    }
    return local.filter { it.remoteOrderId == null } + server
}

internal fun orderError(error: Exception): String = when (error) {
    is retrofit2.HttpException -> when (error.code()) {
        401, 403 -> "Clave administrativa incorrecta o no configurada en el servidor."
        404 -> "El pedido o endpoint no existe en el servidor."
        409 -> "El pedido ya no admite ese cambio. Actualiza el historial."
        else -> "El servidor respondió con error HTTP ${error.code()}."
    }
    is java.io.IOException -> "No se pudo contactar con el servidor. Comprueba Spring Boot y la conexión."
    else -> error.message ?: "No se pudo consultar o actualizar el pedido."
}
