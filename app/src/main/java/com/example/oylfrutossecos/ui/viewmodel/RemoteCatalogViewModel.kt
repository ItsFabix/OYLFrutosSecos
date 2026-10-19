package com.example.oylfrutossecos.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oylfrutossecos.data.remote.ApiHealth
import com.example.oylfrutossecos.data.remote.ApiOrderItemRequest
import com.example.oylfrutossecos.data.remote.ApiOrderRequest
import com.example.oylfrutossecos.data.remote.ApiOrderResponse
import com.example.oylfrutossecos.data.remote.ApiProduct
import com.example.oylfrutossecos.data.remote.OylRestService
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RemoteCatalogState(
    val loading: Boolean = false,
    val health: ApiHealth? = null,
    val products: List<ApiProduct> = emptyList(),
    val error: String? = null,
    val creatingOrder: Boolean = false,
    val requestId: String? = null,
    val order: ApiOrderResponse? = null,
    val orderError: String? = null,
    val catalogWarning: String? = null
)

class RemoteCatalogViewModel(
    private val service: OylRestService,
    context: Context
) : ViewModel() {
    private val preferences = context.applicationContext
        .getSharedPreferences("oyl_api_demo_order", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(
        RemoteCatalogState(requestId = preferences.getString("request_id", null))
    )
    val state = mutableState.asStateFlow()

    init { load() }

    fun load() {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null, catalogWarning = null) }
            try {
                val health = service.health()
                val products = service.products()
                mutableState.update {
                    it.copy(health = health, products = products, loading = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mutableState.update {
                    it.copy(loading = false, error = "No se pudo conectar al servidor: " +
                        (exception.localizedMessage ?: "Error de red"))
                }
            }
        }
    }

    /**
     * Envío aislado de demostración: compra una bolsa de almendras (producto #1).
     * Nunca llama a SQLite ni al checkout de la aplicación.
     *
     * El requestId se guarda ANTES de enviar para poder repetir la misma solicitud
     * si se interrumpe la conexión, sin generar un segundo descuento remoto.
     */
    fun createDemoOrder() {
        val current = mutableState.value
        if (current.creatingOrder || current.loading || current.health == null) return
        viewModelScope.launch {
            val requestId = preferences.getString("request_id", null)
                ?: UUID.randomUUID().toString().also { newId ->
                    if (!preferences.edit().putString("request_id", newId).commit()) {
                        mutableState.update {
                            it.copy(orderError = "No se pudo guardar el ID de la solicitud en el dispositivo.")
                        }
                        return@launch
                    }
                }
            mutableState.update {
                it.copy(creatingOrder = true, requestId = requestId,
                    orderError = null, catalogWarning = null)
            }
            try {
                val order = service.createOrder(
                    ApiOrderRequest(
                        requestId = requestId,
                        customerAlias = "cliente-android-demo",
                        items = listOf(ApiOrderItemRequest(productId = 1L, quantity = 1))
                    )
                )
                mutableState.update { it.copy(order = order, creatingOrder = false) }
                try {
                    val products = service.products()
                    mutableState.update { it.copy(products = products) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.update {
                        it.copy(catalogWarning = "Pedido registrado, pero no se pudo actualizar " +
                            "el stock mostrado. Pulsa Consultar servidor.")
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                // No borrar requestId: un error de red puede ocurrir DESPUÉS de crear el pedido.
                mutableState.update {
                    it.copy(creatingOrder = false,
                        orderError = "No se confirmó el pedido: " +
                            (exception.localizedMessage ?: "Error de red") +
                            ". Reintenta con la MISMA solicitud para evitar duplicados.")
                }
            }
        }
    }

    /** Solo permite preparar un ID nuevo cuando el pedido previo fue confirmado. */
    fun prepareAnotherDemoOrder() {
        val current = mutableState.value
        if (current.creatingOrder || current.order == null) return
        if (preferences.edit().remove("request_id").commit()) {
            mutableState.update {
                it.copy(requestId = null, order = null, orderError = null)
            }
        }
    }

    class Factory(
        private val service: OylRestService,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RemoteCatalogViewModel::class.java))
            return RemoteCatalogViewModel(service, context) as T
        }
    }
}
