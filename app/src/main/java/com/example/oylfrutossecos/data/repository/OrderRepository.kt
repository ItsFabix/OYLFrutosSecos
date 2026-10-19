package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.local.RemoteCheckoutCache
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.*
import com.google.gson.Gson
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

data class PlacedRemoteOrder(val localId: Long, val serverId: String)

/** Spring Boot es la fuente de pedidos y stock; SQLite conserva el historial local. */
class OrderRepository(
    private val database: OylDatabase,
    private val api: OylRestService
) {
    private val cache = RemoteCheckoutCache(database)
    private val gson = Gson()

    suspend fun placeOrder(user: User, delivery: DeliveryMethod,
                           expectedItems: List<CartItem>): PlacedRemoteOrder =
        withContext(Dispatchers.IO) {
            require(user.role == UserRole.CLIENT) { "Solo los clientes pueden realizar pedidos." }
            val pending = cache.pending(user.id) ?: run {
                require(expectedItems.isNotEmpty()) { "Tu carrito está vacío." }
                val current = database.readCart(user.id)
                require(cache.fingerprint(current) == cache.fingerprint(expectedItems)) {
                    "El carrito cambió. Vuelve a revisar el pedido."
                }
                require(current.size <= 30 && current.all { it.quantity in 1..100 }) {
                    "El servidor admite hasta 30 productos y 100 bolsas por producto."
                }
                val request = ApiOrderRequest(
                    requestId = UUID.randomUUID().toString(),
                    customerAlias = user.alias,
                    items = current.map { ApiOrderItemRequest(it.product.id, it.quantity) },
                    delivery = delivery.name,
                    contactPhone = user.phone,
                    contactAddress = if (delivery == DeliveryMethod.DELIVERY) user.address
                        else "Retiro en tienda"
                )
                cache.getOrCreate(user.id, request.requestId, gson.toJson(request),
                    delivery, current)
            }
            val request = gson.fromJson(pending.json, ApiOrderRequest::class.java)
            try {
                val response = api.createOrder(request)
                check(response.requestId == pending.requestId) {
                    "El servidor devolvió un identificador incorrecto."
                }
                // Primero registrar copia local: si falla, el requestId queda intacto.
                val localId = cache.complete(user, response)
                try {
                    cache.refreshProductCache(api.products())
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Pedido confirmado: un error al refrescar el catálogo no deshace la compra.
                }
                PlacedRemoteOrder(localId, response.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (http: HttpException) {
                // El backend no registró un pedido para rechazos de validación/stock.
                if (http.code() == 400 || http.code() == 404 || http.code() == 409) {
                    cache.clearRejected(user.id, pending.requestId)
                }
                throw IllegalStateException(when (http.code()) {
                    400 -> "El servidor rechazó los datos del pedido. Revisa los productos."
                    404 -> "El servidor ya no tiene alguno de los productos solicitados."
                    409 -> "Stock insuficiente en el servidor. Actualiza tu carrito y vuelve a intentar."
                    else -> "El servidor devolvió error ${http.code()}. Intenta de nuevo con la misma solicitud."
                })
            } catch (exception: Exception) {
                throw IllegalStateException(
                    "No fue posible confirmar el pedido. Si el servidor lo recibió, " +
                    "reintentar recuperará la misma solicitud, sin cobrar ni descontar dos veces."
                , exception)
            }
        }

    suspend fun getOrders(user: User): List<OrderSummary> = withContext(Dispatchers.IO) {
        database.readOrders(if (user.role == UserRole.ADMIN) null else user.id)
    }

    suspend fun advanceStatus(orderId: Long): Unit = withContext(Dispatchers.IO) {
        // Solo aplicable a solicitudes históricas locales. El servidor gestiona las nuevas.
        database.advanceDemoOrder(orderId)
    }
}
