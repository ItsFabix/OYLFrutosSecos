package com.example.oylfrutossecos.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.oylfrutossecos.data.model.MovementType
import com.example.oylfrutossecos.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StockAdjustmentState(
    val input: InventoryFormInput, val currentStock: Int,
    val errors: Map<InventoryField, String> = emptyMap(), val saving: Boolean = false,
    val error: String? = null, val completed: Boolean = false
) {
    val locked: Boolean get() = saving || completed
    val projectedStock: Int? get() = InventoryFormValidator.projectedStock(input, currentStock)
}

class StockAdjustmentViewModel(currentStock: Int, private val saved: SavedStateHandle,
    private val persist: (MovementType, Int, String, (String?) -> Unit) -> Unit) : ViewModel() {
    private val mutable = MutableStateFlow(StockAdjustmentState(InventoryFormInput(
        saved["type"] ?: MovementType.ENTRY.name, saved["quantity"] ?: "", saved["note"] ?: ""), currentStock))
    val state = mutable.asStateFlow()

    fun updateStock(stock: Int) { mutable.update { it.copy(currentStock = stock) } }

    fun change(field: InventoryField, value: String) {
        val current = mutable.value
        if (current.locked) return
        val input = when (field) {
            InventoryField.TYPE -> current.input.copy(type = value)
            InventoryField.QUANTITY -> current.input.copy(quantity = value)
            InventoryField.NOTE -> current.input.copy(note = value)
        }
        saved[when (field) {
            InventoryField.TYPE -> "type"; InventoryField.QUANTITY -> "quantity"; InventoryField.NOTE -> "note"
        }] = value
        mutable.value = current.copy(input = input, errors = current.errors - field, error = null)
    }

    fun save() {
        val current = mutable.value
        if (current.locked) return
        val errors = InventoryFormValidator.validate(current.input, current.currentStock)
        mutable.value = current.copy(errors = errors, error = null)
        if (errors.isNotEmpty()) return
        mutable.update { it.copy(saving = true) }
        try {
            persist(InventoryFormValidator.movementType(current.input.type)!!,
                current.input.quantity.trim().toInt(), current.input.note.trim()) { error ->
                mutable.update { it.copy(saving = false, error = error, completed = error == null) }
            }
        } catch (_: Exception) {
                mutable.update { it.copy(saving = false, error = "No se pudo guardar el movimiento. Intenta de nuevo.") }
        }
    }

    class Factory(private val stock: Int,
        private val persist: (MovementType, Int, String, (String?) -> Unit) -> Unit) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(StockAdjustmentViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return StockAdjustmentViewModel(stock, extras.createSavedStateHandle(), persist) as T
        }
    }
}
