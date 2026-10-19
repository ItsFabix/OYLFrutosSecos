package com.example.oylfrutossecos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oylfrutossecos.data.remote.ApiHealth
import com.example.oylfrutossecos.data.remote.ApiProduct
import com.example.oylfrutossecos.data.remote.OylRestService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RemoteCatalogState(
    val loading: Boolean = false,
    val health: ApiHealth? = null,
    val products: List<ApiProduct> = emptyList(),
    val error: String? = null
)

class RemoteCatalogViewModel(private val service: OylRestService) : ViewModel() {
    private val mutableState = MutableStateFlow(RemoteCatalogState())
    val state = mutableState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            mutableState.value = RemoteCatalogState(loading = true)
            try {
                val health = service.health()
                val products = service.products()
                mutableState.value = RemoteCatalogState(
                    health = health,
                    products = products
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mutableState.value = RemoteCatalogState(
                    error = "No se pudo conectar al servidor: ${exception.localizedMessage ?: "Error de red"}"
                )
            }
        }
    }

    class Factory(private val service: OylRestService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RemoteCatalogViewModel::class.java))
            return RemoteCatalogViewModel(service) as T
        }
    }
}
