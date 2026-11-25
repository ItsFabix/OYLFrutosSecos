package com.example.oylfrutossecos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.OrderRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class OrderUiState(
    val orders: List<OrderSummary> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val placedId: Long? = null,
    val placedServerId: String? = null,
    val remoteFresh: Boolean = false
)

class OrderViewModel(private val repository: OrderRepository, private var user: User) : ViewModel() {
    private val mutable = MutableStateFlow(OrderUiState())
    val state = mutable.asStateFlow()
    private var loadJob: Job? = null
    init { load() }

    fun updateUser(updated: User) {
        require(updated.id == user.id && updated.alias == user.alias && updated.role == user.role)
        user = updated
    }

    fun load() {
        if (mutable.value.busy) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = null) }
            try {
                val history = repository.getOrders(user)
                mutable.update { it.copy(orders = history.orders, loading = false,
                    error = history.error, remoteFresh = history.remoteFresh) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.update { it.copy(loading = false, error = "No se pudo consultar el historial.") }
            }
        }
    }

    fun place(delivery: DeliveryMethod, expectedItems: List<CartItem>,
              onSettled: () -> Unit = {}, onSuccess: () -> Unit) {
        if (mutable.value.busy || user.role != UserRole.CLIENT) return
        // Marcar ocupado ANTES de lanzar la coroutine evita confirmar dos veces por doble tap.
        loadJob?.cancel()
        mutable.update { it.copy(busy = true, error = null, placedId = null, placedServerId = null) }
        viewModelScope.launch {
            try {
                val placed = repository.placeOrder(user, delivery, expectedItems)
                // Un fallo posterior al guardado no debe hacer creer al usuario que debe repetirlo.
                mutable.update { it.copy(busy = false, placedId = placed.localId,
                    placedServerId = placed.serverId, loading = false) }
                onSuccess()
                load()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: IllegalArgumentException) {
                mutable.update { it.copy(busy = false, error = e.message ?: "Revisa el carrito.") }
            } catch (e: IllegalStateException) {
                mutable.update { it.copy(busy = false, error = e.message ?: "No se pudo guardar el pedido.") }
            } catch (_: Exception) {
                mutable.update { it.copy(busy = false, error = "No pudimos establecer una conexión. No se confirmó la compra. Si existe un intento pendiente, usa Recuperar pedido pendiente.") }
            } finally {
                mutable.update { it.copy(busy = false) }
                onSettled()
            }
        }
    }

    fun advance(order: OrderSummary) {
        if (user.role != UserRole.ADMIN || mutable.value.busy) return
        if (order.remoteOrderId != null && (!mutable.value.remoteFresh || mutable.value.loading)) return
        loadJob?.cancel()
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repository.advanceStatus(user, order)
                val history = repository.getOrders(user)
                mutable.update { it.copy(busy = false, orders = history.orders, loading = false,
                    error = history.error, remoteFresh = history.remoteFresh) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: IllegalStateException) {
                mutable.update { it.copy(busy = false, error = e.message ?: "Estado no válido.") }
            } catch (e: Exception) {
                mutable.update { it.copy(busy = false, remoteFresh = false,
                    error = com.example.oylfrutossecos.data.repository.restError(e) +
                        " Actualiza el historial para verificar el resultado antes de otro cambio.") }
            }
        }
    }

    fun clearAdminSession() {
        if (user.role != UserRole.ADMIN) return
        loadJob?.cancel()
        mutable.value = OrderUiState(loading = false)
    }

    class Factory(private val repository: OrderRepository, private val user: User): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OrderViewModel::class.java))
            return OrderViewModel(repository, user) as T
        }
    }
}
