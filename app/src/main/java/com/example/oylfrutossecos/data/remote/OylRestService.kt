package com.example.oylfrutossecos.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Header
import retrofit2.http.Path
import okhttp3.RequestBody

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
    val items: List<ApiOrderItem>,
    val delivery: String = "PICKUP",
    val contactPhone: String = "",
    val contactAddress: String = "",
    val createdAt: Long = 0
)

data class ApiProductInput(val name: String, val category: String, val description: String,
    val priceClp: Int, val weightGrams: Int, val stock: Int, val imageKey: String? = null)
data class ApiStockChange(val delta: Int, val note: String)
data class ApiStockResult(val productId: Long, val oldStock: Int, val newStock: Int)
data class ApiInventoryMovement(val id: Long, val productId: Long, val productName: String,
    val delta: Int, val beforeStock: Int, val afterStock: Int, val note: String, val createdAt: Long)

interface OylRestService {
    @GET("api/health")
    suspend fun health(): ApiHealth

    @GET("api/pickup-points")
    suspend fun pickupPoints(): List<com.example.oylfrutossecos.data.model.PickupPoint>

    @GET("api/products")
    suspend fun products(): List<ApiProduct>

    @POST("api/orders")
    suspend fun createOrder(@Body request: ApiOrderRequest): ApiOrderResponse

    @POST("api/orders")
    suspend fun createPersistedOrder(@Body request: RequestBody): ApiOrderResponse

    @GET("api/orders/{id}")
    suspend fun getOrder(@retrofit2.http.Path("id") id: String): ApiOrderResponse

    @GET("api/admin/orders")
    suspend fun adminOrders(@Header("X-OYL-ADMIN-KEY") key: String): List<ApiOrderResponse>

    @POST("api/admin/orders/{id}/advance")
    suspend fun advanceOrder(@Path("id") id: String, @Header("X-OYL-ADMIN-KEY") key: String): ApiOrderResponse

    @POST("api/admin/products")
    suspend fun createProduct(@Body product: ApiProductInput, @Header("X-OYL-ADMIN-KEY") key: String): ApiProduct

    @PUT("api/admin/products/{id}")
    suspend fun editProduct(@Path("id") id: Long, @Body product: ApiProductInput,
        @Header("X-OYL-ADMIN-KEY") key: String): ApiProduct

    @GET("api/admin/inventory/movements")
    suspend fun inventoryMovements(@Header("X-OYL-ADMIN-KEY") key: String): List<ApiInventoryMovement>

    @POST("api/admin/products/{id}/stock")
    suspend fun adjustStock(@Path("id") id: Long, @Body change: ApiStockChange,
        @Header("X-OYL-ADMIN-KEY") key: String): ApiStockResult
}
