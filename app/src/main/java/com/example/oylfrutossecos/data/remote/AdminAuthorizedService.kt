package com.example.oylfrutossecos.data.remote

import retrofit2.HttpException

/** Observes rejected administrative credentials without changing HTTP authorization or retrying writes. */
class AdminAuthorizedService(private val api: OylRestService,
    private val onRejected: suspend (String) -> Unit) : OylRestService by api {
    private suspend fun <T> authorized(key: String, call: suspend () -> T): T = try { call() }
    catch (failure: HttpException) {
        if (failure.code() == 401 || failure.code() == 403) onRejected(key)
        throw failure
    }
    override suspend fun adminOrders(key: String) = authorized(key) { api.adminOrders(key) }
    override suspend fun inventoryMovements(key: String) = authorized(key) { api.inventoryMovements(key) }
    override suspend fun advanceOrder(id: String, key: String) = authorized(key) { api.advanceOrder(id, key) }
    override suspend fun createProduct(product: ApiProductInput, key: String) = authorized(key) { api.createProduct(product, key) }
    override suspend fun editProduct(id: Long, product: ApiProductInput, key: String) = authorized(key) { api.editProduct(id, product, key) }
    override suspend fun adjustStock(id: Long, change: ApiStockChange, key: String) = authorized(key) { api.adjustStock(id, change, key) }
}
