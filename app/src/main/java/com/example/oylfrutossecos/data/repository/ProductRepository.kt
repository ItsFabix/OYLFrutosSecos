package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.local.RemoteCheckoutCache
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.data.model.ProductDraft
import com.example.oylfrutossecos.data.remote.OylRestService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Catálogo H2 oficial, con SQLite como caché disponible sin conexión. */
class ProductRepository(private val database: OylDatabase, private val api: OylRestService) {
    suspend fun getProducts(): List<Product> = withContext(Dispatchers.IO) {
        try {
            RemoteCheckoutCache(database).refreshProductCache(api.products())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Sin Internet o servidor apagado: mostrar último catálogo SQLite.
        }
        database.readProducts()
    }

    // La edición local todavía es una demostración; la gestión oficial será REST admin.
    suspend fun saveProduct(id: Long?, draft: ProductDraft): Long = withContext(Dispatchers.IO) {
        database.saveProduct(id, draft)
    }
}
