package com.example.oylfrutossecos.data.model

// Valores de demostración. Precio en pesos enteros, stock en bolsas.
data class Product(
    val id: Long,
    val name: String,
    val category: String,
    val description: String,
    val priceClp: Int,
    val weightGrams: Int,
    val stock: Int,
    val imageKey: String,
    val remoteId: Long? = null
)
