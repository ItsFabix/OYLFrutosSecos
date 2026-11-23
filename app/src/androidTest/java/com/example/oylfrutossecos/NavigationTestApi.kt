package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.OrderStatus
import com.example.oylfrutossecos.data.remote.*
import com.google.gson.Gson
import okhttp3.RequestBody
import okio.Buffer
import java.util.UUID

/** Doble determinista: el NavHost conserva repositorios y SQLite reales. */
class NavigationTestApi(database: OylDatabase) : StubOylRestService() {
    var acceptedAdminKey = "navigation-test-admin-key"
    private val catalog = database.readProducts().map {
        ApiProduct(it.id, it.name, it.category, it.description, it.priceClp, it.weightGrams, it.stock, it.imageKey)
    }.toMutableList()
    private val orders = linkedMapOf<String, ApiOrderResponse>()
    override suspend fun products() = synchronized(this) { catalog.toList() }
    private fun authorize(key: String) {
        if (key != acceptedAdminKey) throw retrofit2.HttpException(retrofit2.Response.error<Any>(403,
            okhttp3.ResponseBody.create(null, "Forbidden")))
    }
    override suspend fun inventoryMovements(key: String): List<ApiInventoryMovement> {
        authorize(key)
        return emptyList()
    }
    override suspend fun adminOrders(key: String) = synchronized(this) {
        authorize(key)
        orders.values.toList()
    }
    override suspend fun getOrder(id: String) = synchronized(this) { orders.values.first { it.id == id } }
    override suspend fun advanceOrder(id: String, key: String) = synchronized(this) {
        authorize(key)
        val old = orders.values.first { it.id == id }
        old.copy(status = OrderStatus.valueOf(old.status).next()!!.name).also { orders[old.requestId] = it }
    }
    override suspend fun createPersistedOrder(request: RequestBody) = synchronized(this) {
        val json = Buffer().also { request.writeTo(it) }.readUtf8()
        val saved = Gson().fromJson(json, ApiOrderRequest::class.java)
        orders.getOrPut(saved.requestId) {
            val lines = saved.items.map { item ->
                val index = catalog.indexOfFirst { it.id == item.productId }
                val product = catalog[index]
                check(product.stock >= item.quantity)
                catalog[index] = product.copy(stock = product.stock - item.quantity)
                ApiOrderItem(product.id, product.name, item.quantity, product.priceClp)
            }
            ApiOrderResponse(UUID.randomUUID().toString(), saved.requestId, saved.customerAlias, "PENDING",
                lines.sumOf { it.quantity.toLong() * it.unitPriceClp }, lines, saved.delivery,
                saved.contactPhone, saved.contactAddress, System.currentTimeMillis())
        }
    }
}
