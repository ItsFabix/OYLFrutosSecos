package com.example.oylfrutossecos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.data.model.ProductDraft
import com.example.oylfrutossecos.data.repository.ProductRepository
import com.example.oylfrutossecos.domain.CatalogFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogState(
    val products: List<Product> = emptyList(),
    val query: String = "",
    val category: String? = null,
    val loading: Boolean = true,
    val error: String? = null
) {
    val categories: List<String> get() = products.map { it.category }.distinct().sorted()
    val visibleProducts: List<Product> get() = CatalogFilter.apply(products, query, category)
}

class CatalogViewModel(private val repository: ProductRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(CatalogState())
    val state = mutableState.asStateFlow()
    init { load() }

    fun search(query: String) { mutableState.update { it.copy(query = query) } }
    fun selectCategory(category: String?) { mutableState.update { it.copy(category = category) } }
    fun clearFilters() { mutableState.update { it.copy(query = "", category = null) } }

    fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val products = repository.getProducts()
                mutableState.update { it.copy(products = products, loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false,
                    error = "No se pudo cargar el catálogo. Intenta nuevamente.") }
            }
        }
    }

    fun saveProduct(id: Long?, draft: ProductDraft, onResult: (String?) -> Unit) {
        val validation = draft.validationError()
        if (validation != null) { onResult(validation); return }
        viewModelScope.launch {
            try {
                repository.saveProduct(id, draft)
                val products = repository.getProducts()
                mutableState.update { it.copy(products = products, error = null, loading = false) }
                onResult(null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onResult("No se pudo guardar el producto. Intenta de nuevo.")
            }
        }
    }

    class Factory(private val repository: ProductRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CatalogViewModel::class.java))
            return CatalogViewModel(repository) as T
        }
    }
}
