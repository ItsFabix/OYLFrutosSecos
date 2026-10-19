package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.InventoryMovement
import com.example.oylfrutossecos.data.model.MovementType
import com.example.oylfrutossecos.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InventoryRepository(private val database: OylDatabase) {
    suspend fun products(): List<Product> = withContext(Dispatchers.IO) { database.readProducts() }
    suspend fun movements(productId: Long? = null): List<InventoryMovement> = withContext(Dispatchers.IO) {
        database.readInventoryMovements(productId)
    }
    suspend fun adjust(adminId: Long, productId: Long, type: MovementType,
                       quantity: Int, note: String): Long = withContext(Dispatchers.IO) {
        database.recordInventoryMovement(adminId, productId, type, quantity, note)
    }
}
