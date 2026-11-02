package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.data.model.ProductDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** SQLite es la fuente de productos de la evaluación actual. */
class ProductRepository(private val database: OylDatabase) {
    suspend fun getProducts(): List<Product> = withContext(Dispatchers.IO) {
        database.readProducts()
    }

    suspend fun saveProduct(id: Long?, draft: ProductDraft): Long = withContext(Dispatchers.IO) {
        database.saveProduct(id, draft)
    }
}
