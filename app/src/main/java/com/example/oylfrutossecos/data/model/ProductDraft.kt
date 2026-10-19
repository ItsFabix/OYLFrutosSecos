package com.example.oylfrutossecos.data.model

data class ProductDraft(
    val name: String, val category: String, val description: String,
    val priceClp: Int, val weightGrams: Int, val stock: Int
) {
    fun validationError(): String? = when {
        name.trim().length < 3 -> "Escribe un nombre de al menos 3 caracteres."
        category.trim().isEmpty() -> "Selecciona una categoría."
        description.trim().length < 5 -> "La descripción es muy corta."
        priceClp <= 0 -> "El precio debe ser mayor que cero."
        weightGrams <= 0 -> "El peso debe ser mayor que cero."
        stock < 0 -> "El stock no puede ser negativo."
        else -> null
    }
}
