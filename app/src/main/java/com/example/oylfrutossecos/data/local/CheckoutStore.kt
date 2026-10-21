package com.example.oylfrutossecos.data.local

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.*

data class PendingCheckout(
    val requestId: String,
    val json: String,
    val delivery: DeliveryMethod,
    val fingerprint: String
)

/** Contract shared by the SQLite store and checkout recovery tests. */
interface CheckoutStore {
    fun readCart(userId: Long): List<CartItem>
    fun pending(userId: Long): PendingCheckout?
    fun getOrCreate(userId: Long, id: String, json: String,
                    delivery: DeliveryMethod, cart: List<CartItem>): PendingCheckout
    fun clearRejected(userId: Long, requestId: String)
    fun complete(user: User, remote: ApiOrderResponse): Long
    fun refreshProductCache(products: List<ApiProduct>)
}
