package com.example.oylfrutossecos.data.model

enum class MovementType(val label: String) {
    ENTRY("Entrada"), EXIT("Salida")
}

data class InventoryMovement(
    val id: Long,
    val productId: Long,
    val productName: String,
    val type: MovementType,
    val quantity: Int,
    val beforeStock: Int,
    val afterStock: Int,
    val note: String,
    val createdAt: Long
)
