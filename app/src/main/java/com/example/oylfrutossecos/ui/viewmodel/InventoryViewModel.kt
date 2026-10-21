package com.example.oylfrutossecos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oylfrutossecos.data.model.InventoryMovement
import com.example.oylfrutossecos.data.model.MovementType
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.data.repository.InventoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventoryState(
    val products: List<Product> = emptyList(),
    val movements: List<InventoryMovement> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

class InventoryViewModel(private val repository: InventoryRepository,
                         private val adminId: Long) : ViewModel() {
    private val mutable = MutableStateFlow(InventoryState())
    val state = mutable.asStateFlow()
    init { load() }

    fun load() {
        if (mutable.value.busy) return
        viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = null) }
            try {
                val products = repository.products()
                val movements = repository.movements()
                mutable.update { it.copy(products = products, movements = movements, loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.update { it.copy(loading = false, error = "No se pudo cargar el inventario.") }
            }
        }
    }

    fun adjust(productId: Long, type: MovementType, quantity: Int, note: String,
               onResult: (String?) -> Unit = {}, onSuccess: () -> Unit) {
        if (mutable.value.busy) {
            onResult("Ya hay un movimiento guardándose. Espera e intenta nuevamente.")
            return
        }
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                repository.adjust(adminId, productId, type, quantity, note)
                val products = repository.products()
                val movements = repository.movements()
                mutable.update { it.copy(busy = false, loading = false, products = products,
                    movements = movements, notice = "Movimiento registrado correctamente.") }
                onResult(null)
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: IllegalArgumentException) {
                val message = e.message ?: "Revisa la cantidad."
                mutable.update { it.copy(busy = false, error = message) }
                onResult(message)
            } catch (e: IllegalStateException) {
                val message = e.message ?: "No se pudo guardar el movimiento."
                mutable.update { it.copy(busy = false, error = message) }
                onResult(message)
            } catch (_: Exception) {
                val message = "Error al registrar el movimiento local."
                mutable.update { it.copy(busy = false, error = message) }
                onResult(message)
            } finally {
                mutable.update { it.copy(busy = false) }
            }
        }
    }

    fun clearNotice() { mutable.update { it.copy(notice = null, error = null) } }

    class Factory(private val repository: InventoryRepository,
                  private val adminId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(InventoryViewModel::class.java))
            return InventoryViewModel(repository, adminId) as T
        }
    }
}
