package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.local.PendingCheckout
import com.example.oylfrutossecos.data.model.CartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CartRepository(private val database: OylDatabase) {
    data class Snapshot(val items: List<CartItem>, val pending: PendingCheckout?)

    suspend fun snapshot(userId: Long): Snapshot = withContext(Dispatchers.IO) {
        val db = database.readableDatabase
        db.beginTransaction()
        try {
            val result = Snapshot(database.readCart(userId), database.readPendingCheckout(userId))
            db.setTransactionSuccessful()
            result
        } finally { db.endTransaction() }
    }

    suspend fun getCart(userId: Long): List<CartItem> = withContext(Dispatchers.IO) {
        database.readCart(userId)
    }
    suspend fun setQuantity(userId: Long, productId: Long, quantity: Int) =
        withContext(Dispatchers.IO) { database.setCartQuantity(userId, productId, quantity) }
}
