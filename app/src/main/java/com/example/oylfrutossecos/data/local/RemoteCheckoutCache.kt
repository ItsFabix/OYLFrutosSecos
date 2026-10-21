package com.example.oylfrutossecos.data.local

import android.content.ContentValues
import com.example.oylfrutossecos.data.model.CartItem
import com.example.oylfrutossecos.data.model.DeliveryMethod
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.data.remote.ApiOrderResponse
import com.example.oylfrutossecos.data.remote.ApiProduct
import com.example.oylfrutossecos.data.remote.ApiOrderRequest
import com.example.oylfrutossecos.domain.CheckoutLines
import com.google.gson.Gson

/**
 * Almacenamiento de intentos de checkout y copias de pedidos remotos.
 * Comparte la misma base SQLite, sin modificar pedidos antiguos ni fotos privadas.
 */
class RemoteCheckoutCache(private val database: OylDatabase) : CheckoutStore {
    override fun readCart(userId: Long): List<CartItem> = database.readCart(userId)

    fun fingerprint(items: List<CartItem>): String =
        CheckoutLines.fingerprint(items)

    override fun pending(userId: Long): PendingCheckout? {
        database.readableDatabase.query(
            "pending_remote_checkout",
            arrayOf("request_id", "request_json", "delivery", "cart_fingerprint"),
            "user_id = ?", arrayOf(userId.toString()), null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return PendingCheckout(cursor.getString(0), cursor.getString(1),
                DeliveryMethod.valueOf(cursor.getString(2)), cursor.getString(3))
        }
    }

    override fun getOrCreate(userId: Long, id: String, json: String,
                    delivery: DeliveryMethod, cart: List<CartItem>): PendingCheckout {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            val existing = pending(userId)
            if (existing != null) {
                db.setTransactionSuccessful()
                return existing
            }
            // Freeze the same cart we validated; a concurrent edit must not change the payload.
            val current = database.readCart(userId)
            require(fingerprint(current) == fingerprint(cart) && current.isNotEmpty()) {
                "El carrito cambió. Vuelve a revisar el pedido."
            }
            require(current.all { it.quantity in 1..it.product.stock }) {
                "Stock insuficiente. Actualiza el carrito antes de crear un pedido nuevo."
            }
            val fp = fingerprint(current)
            db.insertOrThrow("pending_remote_checkout", null, ContentValues().apply {
                put("user_id", userId)
                put("request_id", id)
                put("request_json", json)
                put("delivery", delivery.name)
                put("cart_fingerprint", fp)
                put("created_at", System.currentTimeMillis())
            })
            db.setTransactionSuccessful()
            return PendingCheckout(id, json, delivery, fp)
        } finally { db.endTransaction() }
    }

    /** Seguro solo para errores HTTP que aseguran que el pedido no fue creado. */
    override fun clearRejected(userId: Long, requestId: String) {
        database.writableDatabase.delete(
            "pending_remote_checkout", "user_id = ? AND request_id = ?",
            arrayOf(userId.toString(), requestId)
        )
    }

    override fun complete(user: User, remote: ApiOrderResponse): Long {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            val pending = pending(user.id)
                ?: error("No hay intento pendiente; revisa el historial antes de continuar.")
            check(pending.requestId == remote.requestId) {
                "El servidor devolvió una solicitud diferente."
            }
            require(remote.totalClp in 1..Int.MAX_VALUE.toLong() &&
                    remote.items.isNotEmpty()) { "Respuesta de pedido inválida." }
            val request = Gson().fromJson(pending.json, ApiOrderRequest::class.java)
            val purchased = CheckoutLines.fingerprintOfRequest(request.items)
            check(request.requestId == pending.requestId &&
                CheckoutLines.normalizeStored(pending.fingerprint) == purchased &&
                CheckoutLines.fingerprintOfRequest(remote.items.map {
                    com.example.oylfrutossecos.data.remote.ApiOrderItemRequest(it.productId, it.quantity)
                }) == purchased) { "Las líneas confirmadas no corresponden a la solicitud guardada." }
            val existing = db.rawQuery(
                "SELECT id, user_id FROM demo_orders WHERE server_order_id = ?",
                arrayOf(remote.id)
            ).use {
                if (it.moveToFirst()) {
                    check(it.getLong(1) == user.id) { "El pedido pertenece a otra cuenta local." }
                    it.getLong(0)
                } else null
            }
            val localId = existing ?: run {
                val id = db.insertOrThrow("demo_orders", null, ContentValues().apply {
                    put("user_id", user.id)
                    put("customer_name", user.name)
                    put("created_at", System.currentTimeMillis())
                    put("status", remote.status)
                    put("delivery", pending.delivery.name)
                    put("phone", user.phone)
                    put("address", if (pending.delivery == DeliveryMethod.DELIVERY)
                        user.address else "Retiro en tienda")
                    put("total_clp", remote.totalClp)
                    put("server_order_id", remote.id)
                })
                check(id > 0L) { "No se pudo cachear el pedido remoto." }
                remote.items.forEach { line ->
                    db.insertOrThrow("demo_order_lines", null, ContentValues().apply {
                        put("order_id", id)
                        put("product_id", line.productId)
                        put("product_name", line.productName)
                        put("quantity", line.quantity)
                        put("unit_price_clp", line.unitPriceClp)
                    })
                }
                id
            }
            // Remove only the purchased lines of this user, independent of price changes.
            // Preserve any unexpected lines/quantities rather than clearing the entire cart.
            request.items.forEach { line ->
                db.delete("cart_items", "user_id = ? AND product_id = ? AND quantity = ?",
                    arrayOf(user.id.toString(), line.productId.toString(), line.quantity.toString()))
            }
            db.delete("pending_remote_checkout", "user_id = ? AND request_id = ?",
                arrayOf(user.id.toString(), pending.requestId))
            db.setTransactionSuccessful()
            return localId
        } finally { db.endTransaction() }
    }

    /** Actualiza stock y precios de productos existentes, sin cambiar image_key. */
    override fun refreshProductCache(products: List<ApiProduct>) {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            products.forEach { product ->
                db.update("products", ContentValues().apply {
                    put("name", product.name)
                    put("category", product.category)
                    put("description", product.description)
                    put("price_clp", product.priceClp)
                    put("weight_grams", product.weightGrams)
                    put("stock", product.stock)
                }, "id = ?", arrayOf(product.id.toString()))
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
