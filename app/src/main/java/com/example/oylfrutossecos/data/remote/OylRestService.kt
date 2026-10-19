package com.example.oylfrutossecos.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class ApiHealth(val status: String, val service: String, val mode: String)

data class ApiProduct(
    val id: Long,
    val name: String,
    val category: String,
    val description: String,
    val priceClp: Int,
    val weightGrams: Int,
    val stock: Int,
    val imageKey: String
)

// Solo para pedidos de demostración: no se integra todavía con el carrito SQLite.
data class ApiOrderItemRequest(val productId: Long, val quantity: Int)
data class ApiOrderRequest(
    val requestId: String,
    val customerAlias: String,
    val items: List<ApiOrderItemRequest>,
    val delivery: String = "PICKUP",
    val contactPhone: String = "",
    val contactAddress: String = ""
)
data class ApiOrderItem(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPriceClp: Int
)
data class ApiOrderResponse(
    val id: String,
    val requestId: String,
    val customerAlias: String,
    val status: String,
    val totalClp: Long,
    val items: List<ApiOrderItem>
)

interface OylRestService {
    @GET("api/health")
    suspend fun health(): ApiHealth

    @GET("api/products")
    suspend fun products(): List<ApiProduct>

    @POST("api/orders")
    suspend fun createOrder(@Body request: ApiOrderRequest): ApiOrderResponse

    @GET("api/orders/{id}")
    suspend fun getOrder(@retrofit2.http.Path("id") id: String): ApiOrderResponse
}
