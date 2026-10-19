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

data class OrderUiState(
    val orders: List<OrderSummary> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val placedId: Long? = null
)

class OrderViewModel(private val repository: OrderRepository, private val user: User) : ViewModel() {
    private val mutable = MutableStateFlow(OrderUiState())
    val state = mutable.asStateFlow()
    init { load() }

    fun load() {
        if (mutable.value.busy) return
        viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = null) }
            try {
                val orders = repository.getOrders(user)
                mutable.update { it.copy(orders = orders, loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.update { it.copy(loading = false, error = "No se pudo consultar el historial.") }
            }
        }
    }

    fun place(delivery: DeliveryMethod, expectedItems: List<CartItem>, onSuccess: () -> Unit) {
        if (mutable.value.busy || expectedItems.isEmpty() || user.role != UserRole.CLIENT) return
        // Marcar ocupado ANTES de lanzar la coroutine evita confirmar dos veces por doble tap.
        mutable.update { it.copy(busy = true, error = null, placedId = null) }
        viewModelScope.launch {
            try {
                val id = repository.placeOrder(user, delivery, expectedItems)
                // Un fallo posterior al guardado no debe hacer creer al usuario que debe repetirlo.
                mutable.update { it.copy(busy = false, placedId = id, loading = false) }
                onSuccess()
                load()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: IllegalArgumentException) {
                mutable.update { it.copy(busy = false, error = e.message ?: "Revisa el carrito.") }
            } catch (e: IllegalStateException) {
                mutable.update { it.copy(busy = false, error = e.message ?: "No se pudo guardar el pedido.") }
            } catch (_: Exception) {
                mutable.update { it.copy(busy = false, error = "No se pudo guardar el pedido de prueba.") }
            }
        }
    }

    fun advance(orderId: Long) {
        if (user.role != UserRole.ADMIN || mutable.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repository.advanceStatus(orderId)
                val orders = repository.getOrders(user)
                mutable.update { it.copy(busy = false, orders = orders, loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: IllegalStateException) {
                mutable.update { it.copy(busy = false, error = e.message ?: "Estado no válido.") }
            } catch (_: Exception) {
                mutable.update { it.copy(busy = false, error = "No se pudo actualizar el pedido.") }
            }
        }
    }

    class Factory(private val repository: OrderRepository, private val user: User): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OrderViewModel::class.java))
            return OrderViewModel(repository, user) as T
        }
    }
}
