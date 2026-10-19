package com.example.oylfrutossecos.data.model

data class CartItem(val product: Product, val quantity: Int) {
    val subtotalClp: Long get() = product.priceClp.toLong() * quantity
}
