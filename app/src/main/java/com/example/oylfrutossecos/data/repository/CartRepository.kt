package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.CartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CartRepository(private val database: OylDatabase) {
    suspend fun getCart(userId: Long): List<CartItem> = withContext(Dispatchers.IO) {
        database.readCart(userId)
    }
    suspend fun setQuantity(userId: Long, productId: Long, quantity: Int) =
        withContext(Dispatchers.IO) { database.setCartQuantity(userId, productId, quantity) }
}
