package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.local.RemoteCheckoutCache
import com.example.oylfrutossecos.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlacedRemoteOrder(val localId: Long, val serverId: String)

/** Los pedidos nuevos se confirman exclusivamente en Spring Boot. */
class OrderRepository(private val database: OylDatabase, private val api: OylRestService,
    private val adminKey: () -> String = { "" }) {
    private val checkout = CheckoutCoordinator(RemoteCheckoutCache(database), api)
    suspend fun placeOrder(user: User, delivery: DeliveryMethod,
                           expectedItems: List<CartItem>): PlacedRemoteOrder = withContext(Dispatchers.IO) {
        require(user.role == UserRole.CLIENT) { "Solo los clientes pueden realizar pedidos." }
        if (database.readPendingCheckout(user.id) == null) {
            RemoteCheckoutCache(database).refreshProductCache(api.products())
            require(database.readCart(user.id).all { it.product.remoteId != null }) {
                "Algunos productos del carrito ya no están disponibles. Quítalos y vuelve a elegirlos en el catálogo."
            }
        }
        checkout.placeOrder(user, delivery, expectedItems)
    }

    suspend fun getOrders(user: User): OrderHistory = withContext(Dispatchers.IO) {
        val local = database.readOrders(if (user.role == UserRole.ADMIN) null else user.id)
        try {
            val remote = if (user.role == UserRole.ADMIN) {
                check(adminKey().isNotBlank()) { "Valida tu acceso administrativo para consultar todos los pedidos." }
                api.adminOrders(adminKey())
            } else local.mapNotNull { it.remoteOrderId }.distinct().map { api.getOrder(it) }
            if (user.role == UserRole.CLIENT) remote.forEach {
                database.updateRemoteOrderStatus(user.id, it.id, it.status)
            }
            OrderHistory(mergeOrderHistory(local, remote), remoteFresh = true)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (e: Exception) {
            OrderHistory(local, restError(e) + " Los datos guardados pueden estar desactualizados.")
        }
    }

    suspend fun advanceStatus(user: User, order: OrderSummary): Unit = withContext(Dispatchers.IO) {
        require(user.role == UserRole.ADMIN) { "Solo el administrador puede cambiar estados." }
        if (order.remoteOrderId == null) database.advanceDemoOrder(order.id)
        else {
            check(adminKey().isNotBlank()) { "Introduce la clave administrativa." }
            api.advanceOrder(order.remoteOrderId, adminKey())
        }
    }
}

data class OrderHistory(val orders: List<OrderSummary>, val error: String? = null, val remoteFresh: Boolean = false)

internal fun mergeOrderHistory(local: List<OrderSummary>, remote: List<ApiOrderResponse>): List<OrderSummary> {
    val cached = local.mapNotNull { row -> row.remoteOrderId?.let { it to row } }.toMap()
    val server = remote.distinctBy { it.id }.mapIndexed { index, row ->
        val previous = cached[row.id]
        OrderSummary(previous?.id ?: -(index + 1L), previous?.userId ?: 0L,
            row.customerAlias, row.createdAt.takeIf { it > 0 } ?: previous?.createdAt ?: 0L,
            OrderStatus.valueOf(row.status), DeliveryMethod.valueOf(row.delivery),
            row.contactPhone, row.contactAddress, row.totalClp,
            row.items.map { OrderLine(it.productId, it.productName, it.quantity, it.unitPriceClp) }, row.id)
    }
    return (local.filter { it.remoteOrderId == null } + server).sortedByDescending { it.createdAt }
}

internal fun restError(error: Exception): String = when (error) {
    is retrofit2.HttpException -> when (error.code()) {
        401, 403 -> "La clave administrativa no es válida. Configura nuevamente tu acceso."
        404 -> "No encontramos la información solicitada. Actualiza e inténtalo nuevamente."
        409 -> "No pudimos aplicar el cambio. Actualiza los datos antes de reintentar."
        else -> "Ocurrió un problema. Inténtalo más tarde."
    }
    is java.io.IOException -> "No pudimos establecer una conexión. Inténtalo nuevamente."
    else -> "No pudimos completar la operación. Inténtalo nuevamente."
}
