package com.example.oylfrutossecos.domain

object CartRules {
    fun canSetQuantity(quantity: Int, stock: Int): Boolean = quantity in 1..stock
    fun total(pricesAndQuantities: List<Pair<Int, Int>>): Long =
        pricesAndQuantities.sumOf { (price, quantity) -> price.toLong() * quantity }
}
