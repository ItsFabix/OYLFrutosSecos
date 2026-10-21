package com.example.oylfrutossecos.domain

import com.example.oylfrutossecos.data.model.MovementType

enum class InventoryField { TYPE, QUANTITY, NOTE }

data class InventoryFormInput(
    val type: String = MovementType.ENTRY.name, val quantity: String = "", val note: String = ""
)

object InventoryFormValidator {
    fun movementType(raw: String): MovementType? = MovementType.entries.firstOrNull { it.name == raw }

    fun validate(input: InventoryFormInput, currentStock: Int): Map<InventoryField, String> = buildMap {
        val type = movementType(input.type)
        if (type == null) put(InventoryField.TYPE, "Selecciona Entrada o Salida.")
        val raw = input.quantity.trim()
        val amount = raw.toIntOrNull()
        val quantityError = when {
            raw.isEmpty() -> "Ingresa la cantidad de bolsas."
            !Regex("-?[0-9]+").matches(raw) -> "La cantidad debe ser un número entero, sin decimales."
            raw.startsWith('-') -> "La cantidad no puede ser negativa."
            amount == null || amount !in 1..100000 -> "La cantidad debe estar entre 1 y 100.000 bolsas."
            type != null -> runCatching { InventoryRules.newStock(currentStock, type, amount) }.exceptionOrNull()?.message
            else -> null
        }
        quantityError?.let { put(InventoryField.QUANTITY, it) }
        InventoryRules.validateNote(input.note)?.let { put(InventoryField.NOTE, it) }
    }

    fun projectedStock(input: InventoryFormInput, currentStock: Int): Int? {
        val type = movementType(input.type) ?: return null
        val quantity = input.quantity.trim().toIntOrNull() ?: return null
        return runCatching { InventoryRules.newStock(currentStock, type, quantity) }.getOrNull()
    }
}
