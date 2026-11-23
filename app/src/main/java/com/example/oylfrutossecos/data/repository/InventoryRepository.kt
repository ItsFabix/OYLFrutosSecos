package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.InventoryMovement
import com.example.oylfrutossecos.data.model.MovementType
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InventoryRepository(private val database: OylDatabase, private val api: OylRestService,
    private val adminKey: () -> String = { "" }) {
    suspend fun products(): List<Product> = ProductRepository(database, api, adminKey).getProducts()
    suspend fun movements(productId: Long? = null): List<InventoryMovement> = withContext(Dispatchers.IO) {
        check(adminKey().isNotBlank()) { "Introduce la clave administrativa para consultar los movimientos REST." }
        val products = database.readProducts().mapNotNull { product -> product.remoteId?.let { it to product.id } }.toMap()
        api.inventoryMovements(adminKey()).map { movement ->
            InventoryMovement(movement.id, products[movement.productId] ?: movement.productId, movement.productName,
                if (movement.delta > 0) MovementType.ENTRY else MovementType.EXIT, kotlin.math.abs(movement.delta),
                movement.beforeStock, movement.afterStock, movement.note, movement.createdAt)
        }.filter { productId == null || it.productId == productId }
    }
    suspend fun adjust(adminId: Long, productId: Long, type: MovementType,
                       quantity: Int, note: String): Long = withContext(Dispatchers.IO) {
        check(adminKey().isNotBlank()) { "Introduce la clave administrativa para registrar movimientos." }
        require(quantity in 1..10000 && note.trim().length in 3..200) { "Revisa la cantidad y el motivo del movimiento." }
        val remote = database.readProducts().firstOrNull { it.id == productId }?.remoteId
            ?: error("Producto sin vínculo REST.")
        api.adjustStock(remote, ApiStockChange(if (type == MovementType.ENTRY) quantity else -quantity, note.trim()), adminKey())
        // El servidor ya confirmó: un fallo de refresco no convierte el movimiento en fallido.
        try { products() } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { }
        productId
    }
}
