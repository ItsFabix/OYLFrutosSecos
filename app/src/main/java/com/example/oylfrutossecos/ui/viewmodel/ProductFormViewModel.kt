package com.example.oylfrutossecos.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.data.model.ProductDraft
import com.example.oylfrutossecos.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProductFormState(
    val input: ProductFormInput, val isNew: Boolean, val existingImageKey: String?,
    val errors: Map<ProductField, String> = emptyMap(), val saving: Boolean = false,
    val error: String? = null, val completed: Boolean = false,
    val selectedPhoto: String? = null, val pendingPhoto: String? = null
) {
    val locked: Boolean get() = saving || pendingPhoto != null || completed
    val imageKey: String? get() = selectedPhoto?.let { "photo:$it" } ?: existingImageKey
}

class ProductFormViewModel(
    private val product: Product?, isNew: Boolean, private val saved: SavedStateHandle,
    private val persist: (ProductDraft, (String?) -> Unit) -> Unit
) : ViewModel() {
    private val mutable = MutableStateFlow(ProductFormState(
        input = ProductFormInput(
            saved["name"] ?: product?.name.orEmpty(), saved["category"] ?: product?.category ?: "Frutos secos",
            saved["description"] ?: product?.description.orEmpty(),
            saved["price"] ?: product?.priceClp?.toString().orEmpty(),
            saved["weight"] ?: product?.weightGrams?.toString().orEmpty(),
            saved["stock"] ?: product?.stock?.toString() ?: "0"),
        isNew = isNew, existingImageKey = product?.imageKey,
        selectedPhoto = saved["selectedPhoto"], pendingPhoto = saved["pendingPhoto"]))
    val state = mutable.asStateFlow()

    fun change(field: ProductField, value: String) {
        val current = mutable.value
        if (current.locked || (field == ProductField.STOCK && !current.isNew)) return
        val input = when (field) {
            ProductField.NAME -> current.input.copy(name = value)
            ProductField.CATEGORY -> current.input.copy(category = value)
            ProductField.DESCRIPTION -> current.input.copy(description = value)
            ProductField.PRICE -> current.input.copy(price = value)
            ProductField.WEIGHT -> current.input.copy(weight = value)
            ProductField.STOCK -> current.input.copy(initialStock = value)
        }
        saved[when (field) {
            ProductField.NAME -> "name"; ProductField.CATEGORY -> "category"
            ProductField.DESCRIPTION -> "description"; ProductField.PRICE -> "price"
            ProductField.WEIGHT -> "weight"; ProductField.STOCK -> "stock"
        }] = value
        mutable.value = current.copy(input = input, errors = current.errors - field, error = null)
    }

    fun save() {
        val current = mutable.value
        if (current.locked) return
        val errors = ProductValidator.validate(current.input, current.isNew)
        mutable.value = current.copy(errors = errors, error = null)
        if (errors.isNotEmpty()) return
        val draft = ProductValidator.toDraft(current.input, current.isNew, product?.stock ?: 0,
            current.selectedPhoto?.let { "photo:$it" })
        // Synchronous guard: a second tap cannot enqueue another operation.
        mutable.update { it.copy(saving = true) }
        try {
            persist(draft) { error ->
                mutable.update { it.copy(saving = false, error = error, completed = error == null) }
            }
        } catch (_: Exception) {
            mutable.update { it.copy(saving = false, error = "No se pudo guardar el producto. Intenta de nuevo.") }
        }
    }

    fun beginPhotoCapture(name: String): Boolean {
        if (mutable.value.locked || "photo:$name" == mutable.value.existingImageKey ||
            name == mutable.value.selectedPhoto) return false
        saved["pendingPhoto"] = name
        mutable.update { it.copy(pendingPhoto = name, error = null) }
        return true
    }

    /** Returns only unsaved temporary candidates; never the existing product photograph. */
    fun finishPhotoCapture(success: Boolean): List<String> {
        val current = mutable.value
        val pending = current.pendingPhoto ?: return emptyList()
        val selected = if (success) pending else current.selectedPhoto
        saved["pendingPhoto"] = null
        saved["selectedPhoto"] = selected
        mutable.value = current.copy(pendingPhoto = null, selectedPhoto = selected)
        return listOfNotNull(if (success) current.selectedPhoto?.takeUnless { it == pending } else pending)
            .filter { "photo:$it" != current.existingImageKey }
    }

    fun cameraFailed(): List<String> {
        val temporary = finishPhotoCapture(false)
        mutable.update { it.copy(error = "No se pudo abrir la cámara. La fotografía anterior se conserva.") }
        return temporary
    }

    fun discardPhoto(): List<String> {
        val current = mutable.value
        if (current.locked) return emptyList()
        saved["selectedPhoto"] = null
        mutable.value = current.copy(selectedPhoto = null)
        return listOfNotNull(current.selectedPhoto).filter { "photo:$it" != current.existingImageKey }
    }

    class Factory(private val product: Product?, private val isNew: Boolean,
        private val persist: (ProductDraft, (String?) -> Unit) -> Unit) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(ProductFormViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ProductFormViewModel(product, isNew, extras.createSavedStateHandle(), persist) as T
        }
    }
}
