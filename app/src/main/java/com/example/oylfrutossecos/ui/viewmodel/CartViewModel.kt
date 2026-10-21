package com.example.oylfrutossecos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oylfrutossecos.data.model.CartItem
import com.example.oylfrutossecos.data.local.PendingCheckout
import com.example.oylfrutossecos.data.repository.CartRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartState(
    val items: List<CartItem> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val notice: String? = null,
    val error: String? = null,
    val pending: PendingCheckout? = null
) {
    val total: Long get() = items.sumOf { it.subtotalClp }
    val count: Int get() = items.sumOf { it.quantity }
    val canEdit: Boolean get() = !loading && !busy && pending == null
    val canCheckout: Boolean get() = !loading && !busy &&
        (pending != null || (items.isNotEmpty() && items.none { it.quantity > it.product.stock }))
}

class CartViewModel(private val repository: CartRepository, private val userId: Long) : ViewModel() {
    private val mutable = MutableStateFlow(CartState())
    val state = mutable.asStateFlow()
    init { reload() }

    fun clearMessage() { mutable.update { it.copy(notice = null, error = null) } }

    fun reload() {
        viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = null) }
            try {
                val snapshot = repository.snapshot(userId)
                mutable.update { it.copy(items = snapshot.items, pending = snapshot.pending, loading = false,
                    notice = if (snapshot.items.isEmpty() || snapshot.pending != null) null else it.notice) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.update { it.copy(loading = false, error = "No se pudo leer el carrito.") }
            }
        }
    }

    fun add(productId: Long, amount: Int) {
        val current = mutable.value.items.firstOrNull { it.product.id == productId }?.quantity ?: 0
        change(productId, current + amount, "Producto agregado al carrito.")
    }

    fun change(productId: Long, quantity: Int, confirmation: String? = null) {
        if (!mutable.value.canEdit) return
        mutable.update { it.copy(busy = true, notice = null, error = null) }
        viewModelScope.launch {
            try {
                repository.setQuantity(userId, productId, quantity)
                val items = repository.getCart(userId)
                mutable.update { it.copy(items = items, busy = false, notice = confirmation) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: IllegalArgumentException) {
                mutable.update { it.copy(busy = false, error = e.message ?: "Cantidad no válida.") }
            } catch (e: IllegalStateException) {
                mutable.update { it.copy(busy = false,
                    error = e.message ?: "No se pudo actualizar el carrito.") }
                reload()
            } catch (_: Exception) {
                mutable.update { it.copy(busy = false, error = "No se pudo actualizar el carrito.") }
            }
        }
    }

    class Factory(private val repo: CartRepository, private val userId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CartViewModel::class.java))
            return CartViewModel(repo, userId) as T
        }
    }
}
