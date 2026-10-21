package com.example.oylfrutossecos.domain

import com.example.oylfrutossecos.data.model.ProductDraft

enum class ProductField { NAME, CATEGORY, DESCRIPTION, PRICE, WEIGHT, STOCK }

data class ProductFormInput(
    val name: String = "", val category: String = "Frutos secos", val description: String = "",
    val price: String = "", val weight: String = "", val initialStock: String = "0"
)

/** Reglas de entrada independientes de Android y Compose. */
object ProductValidator {
    val categories = listOf("Frutos secos", "Semillas", "Deshidratados")

    fun validate(input: ProductFormInput, isNew: Boolean): Map<ProductField, String> = buildMap {
        if (input.name.trim().length < 3)
            put(ProductField.NAME, "Escribe un nombre de al menos 3 caracteres.")
        if (input.category !in categories)
            put(ProductField.CATEGORY, "Selecciona una categoría válida de la lista.")
        if (input.description.trim().length < 5)
            put(ProductField.DESCRIPTION, "Escribe una descripción de al menos 5 caracteres.")
        integerError(input.price, "precio", false)?.let { put(ProductField.PRICE, it) }
        integerError(input.weight, "peso", false)?.let { put(ProductField.WEIGHT, it) }
        if (isNew) integerError(input.initialStock, "stock inicial", true)?.let { put(ProductField.STOCK, it) }
    }

    private fun integerError(raw: String, label: String, allowZero: Boolean): String? {
        val value = raw.trim()
        if (value.isEmpty()) return "Ingresa el $label."
        if (!Regex("-?[0-9]+").matches(value)) return "El $label debe ser un número entero, sin decimales."
        if (value.startsWith('-')) return "El $label no puede ser negativo."
        val number = value.toIntOrNull() ?: return "El $label no puede superar 2.147.483.647."
        if (!allowZero && number == 0) return "El $label debe ser mayor que cero."
        return null
    }

    fun validate(draft: ProductDraft): Map<ProductField, String> = validate(ProductFormInput(
        draft.name, draft.category, draft.description, draft.priceClp.toString(),
        draft.weightGrams.toString(), draft.stock.toString()), isNew = true)

    fun toDraft(input: ProductFormInput, isNew: Boolean, currentStock: Int,
                imageKey: String?): ProductDraft {
        require(validate(input, isNew).isEmpty()) { "Revisa los campos del producto." }
        return ProductDraft(input.name.trim(), input.category, input.description.trim(),
            input.price.trim().toInt(), input.weight.trim().toInt(),
            if (isNew) input.initialStock.trim().toInt() else currentStock, imageKey)
    }
}
