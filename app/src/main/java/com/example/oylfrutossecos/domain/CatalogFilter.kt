package com.example.oylfrutossecos.domain

import com.example.oylfrutossecos.data.model.Product
import java.text.Normalizer
import java.util.Locale

object CatalogFilter {
    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()

    fun apply(products: List<Product>, query: String, category: String?): List<Product> {
        val words = normalize(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
        return products.filter { product ->
            val text = normalize("${product.name} ${product.category}")
            (category == null || product.category == category) && words.all { it in text }
        }
    }
}
