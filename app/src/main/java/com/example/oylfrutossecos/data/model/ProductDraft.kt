package com.example.oylfrutossecos.data.model

import com.example.oylfrutossecos.domain.ProductValidator

data class ProductDraft(
    val name: String, val category: String, val description: String,
    val priceClp: Int, val weightGrams: Int, val stock: Int,
    // null conserva la imagen actual o la ilustración predeterminada.
    val imageKey: String? = null
) {
    fun validationError(): String? = ProductValidator.validate(this).values.firstOrNull()
}
