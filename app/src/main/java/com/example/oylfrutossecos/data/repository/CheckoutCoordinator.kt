package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.CheckoutStore
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.*
import com.example.oylfrutossecos.domain.CheckoutLines
import com.google.gson.Gson
import java.util.UUID
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.HttpException

/** Owns only the checkout protocol; all durable state lives in CheckoutStore. */
class CheckoutCoordinator(private val cache: CheckoutStore, private val api: OylRestService) {
    private val gson = Gson()

    suspend fun placeOrder(user: User, delivery: DeliveryMethod,
                           expectedItems: List<CartItem>): PlacedRemoteOrder {
        require(user.role == UserRole.CLIENT) { "Solo los clientes pueden realizar pedidos." }
        val pending = cache.pending(user.id) ?: run {
            require(expectedItems.isNotEmpty()) { "Tu carrito está vacío." }
            val current = cache.readCart(user.id)
            require(CheckoutLines.fingerprint(current) == CheckoutLines.fingerprint(expectedItems)) {
                "El carrito cambió. Vuelve a revisar el pedido."
            }
            require(current.size <= 30 && current.all { it.quantity in 1..100 }) {
                "El servidor admite hasta 30 productos y 100 bolsas por producto."
            }
            val request = ApiOrderRequest(
                UUID.randomUUID().toString(), user.alias,
                current.map { ApiOrderItemRequest(it.product.remoteId ?: it.product.id, it.quantity) },
                delivery.name, user.phone,
                if (delivery == DeliveryMethod.DELIVERY) user.address else "Retiro en tienda"
            )
            cache.getOrCreate(user.id, request.requestId, gson.toJson(request), delivery, current)
        }
        try {
            // Send the exact persisted JSON, including legacy fields/formatting. Never rebuild a retry.
            val response = api.createPersistedOrder(
                RequestBody.create(MediaType.parse("application/json; charset=utf-8"), pending.json))
            check(response.requestId == pending.requestId) {
                "El servidor devolvió un identificador incorrecto."
            }
            val localId = cache.complete(user, response)
            // Confirmation is durable. Catalogue refresh must not turn success into a failed checkout.
            try { cache.refreshProductCache(api.products()) }
            catch (_: Exception) { /* A later catalogue load can refresh prices/stock. */ }
            return PlacedRemoteOrder(localId, response.id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (http: HttpException) {
            // A bare 409 (including from an older backend) is NOT proof of rejection.
            val rejected = http.code() == 409 &&
                http.response()?.headers()?.get(REJECTED_REQUEST_HEADER) == pending.requestId
            if (rejected) cache.clearRejected(user.id, pending.requestId)
            throw IllegalStateException(if (rejected)
                "El servidor confirmó que esta solicitud no se registró por falta de stock. " +
                    "Puedes corregir el carrito y volver a confirmar."
            else "El servidor devolvió error ${http.code()}. La solicitud sigue guardada. " +
                "Usa Recuperar pedido pendiente; no se generará otro identificador.", http)
        } catch (exception: Exception) {
            throw IllegalStateException(
                "No fue posible confirmar el resultado. Usa Recuperar pedido pendiente para " +
                    "reenviar la misma solicitud, incluso si el stock actual es cero.", exception)
        }
    }

    companion object {
        const val REJECTED_REQUEST_HEADER = "X-OYL-Rejected-Request"
    }
}
