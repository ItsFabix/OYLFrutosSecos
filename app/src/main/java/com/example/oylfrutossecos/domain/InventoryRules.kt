package com.example.oylfrutossecos.domain

import com.example.oylfrutossecos.data.model.MovementType

/** Criterios de ejemplo para la gestión local del inventario. */
object InventoryRules {
    const val LOW_STOCK_LIMIT = 5

    fun newStock(current: Int, type: MovementType, amount: Int): Int {
        require(current >= 0) { "El stock actual no es válido." }
        require(amount in 1..100000) { "La cantidad debe estar entre 1 y 100.000 bolsas." }
        return when (type) {
            MovementType.ENTRY -> {
                require(current.toLong() + amount <= Int.MAX_VALUE) { "Se supera el stock máximo." }
                current + amount
            }
            MovementType.EXIT -> {
                require(amount <= current) { "Stock insuficiente: solo quedan $current bolsas." }
                current - amount
            }
        }
    }

    fun validateNote(note: String): String? = when {
        note.trim().length < 3 -> "Escribe un motivo de al menos 3 caracteres."
        note.trim().length > 120 -> "El motivo no puede superar los 120 caracteres."
        else -> null
    }
}
