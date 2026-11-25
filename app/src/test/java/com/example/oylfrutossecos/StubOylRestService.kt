package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.remote.*
import okhttp3.RequestBody

/** Endpoints no utilizados fallan: las pruebas no simulan éxito accidental. */
open class StubOylRestService : OylRestService {
    override suspend fun health(): ApiHealth = error("Unused")
    override suspend fun products(): List<ApiProduct> = error("Unused")
    override suspend fun pickupPoints(): List<com.example.oylfrutossecos.data.model.PickupPoint> = error("Unused")
    override suspend fun orderEvents(id: String): List<ApiOrderEvent> = error("Unused")
    override suspend fun createOrder(request: ApiOrderRequest): ApiOrderResponse = error("Unused")
    override suspend fun createPersistedOrder(request: RequestBody): ApiOrderResponse = error("Unused")
    override suspend fun getOrder(id: String): ApiOrderResponse = error("Unused")
    override suspend fun adminOrders(key: String): List<ApiOrderResponse> = error("Unused")
    override suspend fun advanceOrder(id: String, key: String): ApiOrderResponse = error("Unused")
    override suspend fun createProduct(product: ApiProductInput, key: String): ApiProduct = error("Unused")
    override suspend fun editProduct(id: Long, product: ApiProductInput, key: String): ApiProduct = error("Unused")
    override suspend fun inventoryMovements(key: String): List<ApiInventoryMovement> = error("Unused")
    override suspend fun adjustStock(id: Long, change: ApiStockChange, key: String): ApiStockResult = error("Unused")
}
