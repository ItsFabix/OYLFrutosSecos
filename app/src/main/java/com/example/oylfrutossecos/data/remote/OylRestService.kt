package com.example.oylfrutossecos.data.remote

import retrofit2.http.GET

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

interface OylRestService {
    @GET("api/health")
    suspend fun health(): ApiHealth

    @GET("api/products")
    suspend fun products(): List<ApiProduct>
}
