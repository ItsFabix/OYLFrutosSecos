package com.example.oylfrutossecos.data.repository

import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.data.model.ProductDraft
import com.example.oylfrutossecos.data.local.RemoteCheckoutCache
import com.example.oylfrutossecos.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** REST es la autoridad; SQLite conserva la última copia y las fotos privadas. */
class ProductRepository(private val database: OylDatabase,
    private val api: OylRestService, private val adminKey: () -> String = { "" }) {
    private val cache = RemoteCheckoutCache(database)
    suspend fun getProducts(): List<Product> = withContext(Dispatchers.IO) {
        cache.refreshProductCache(api.products())
        cachedProducts()
    }

    suspend fun cachedProducts(): List<Product> = withContext(Dispatchers.IO) {
        database.readProducts().filter { it.remoteId != null }
    }

    suspend fun saveProduct(id: Long?, draft: ProductDraft): Long = withContext(Dispatchers.IO) {
        require(draft.validationError() == null) { draft.validationError().orEmpty() }
        val key = adminKey().also { check(it.isNotBlank()) { "Introduce la clave administrativa para guardar productos." } }
        val remoteId = id?.let { local -> database.readProducts().firstOrNull { it.id == local }?.remoteId
                ?: error("Este producto ya no está disponible. Actualiza el catálogo.") }
        // Una foto privada no es una URL pública ni debe salir del dispositivo.
        val input = ApiProductInput(draft.name, draft.category, draft.description, draft.priceClp,
            draft.weightGrams, draft.stock, draft.imageKey?.takeIf { it in listOf("nut", "fruit", "seed") })
        val saved = if (remoteId == null) api.createProduct(input, key) else api.editProduct(remoteId, input, key)
        cache.syncProducts(listOf(saved))
        val local = database.readProducts().first { it.remoteId == saved.id }
        if (draft.imageKey != null) database.saveProduct(local.id, ProductDraft(saved.name, saved.category,
            saved.description, saved.priceClp, saved.weightGrams, saved.stock, draft.imageKey))
        local.id
    }
}
