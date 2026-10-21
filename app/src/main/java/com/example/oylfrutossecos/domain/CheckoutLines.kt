package com.example.oylfrutossecos.domain

import com.example.oylfrutossecos.data.model.CartItem
import com.example.oylfrutossecos.data.remote.ApiOrderItemRequest

/** Price is mutable; the identity of purchased lines is product ID + quantity. */
object CheckoutLines {
    fun fingerprint(items: List<CartItem>): String =
        items.sortedBy { it.product.id }.joinToString(";") { "${it.product.id}:${it.quantity}" }

    fun fingerprintOfRequest(items: List<ApiOrderItemRequest>): String =
        items.sortedBy { it.productId }.joinToString(";") { "${it.productId}:${it.quantity}" }

    /** Accepts v7 fingerprints written before this fix (id:quantity:price), without migration. */
    fun normalizeStored(value: String): String? {
        if (value.isBlank()) return null
        val lines = value.split(';').map { line ->
            val parts = line.split(':')
            if (parts.size !in 2..3) return null
            val id = parts[0].toLongOrNull()?.takeIf { it > 0 } ?: return null
            val quantity = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
            if (parts.size == 3 && parts[2].toIntOrNull()?.takeIf { it > 0 } == null) return null
            id to quantity
        }
        if (lines.map { it.first }.distinct().size != lines.size) return null
        return lines.sortedBy { it.first }.joinToString(";") { "${it.first}:${it.second}" }
    }
}
