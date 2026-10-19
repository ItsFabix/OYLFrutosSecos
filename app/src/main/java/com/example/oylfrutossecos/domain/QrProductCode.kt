package com.example.oylfrutossecos.domain

/**
 * Acepta los identificadores que imprimimos en nuestros QR de demostración:
 * OYL:1, OYL:ID:1 o simplemente 1. Nunca interpreta una URL ni ejecuta código.
 */
object QrProductCode {
    private val validNumber = Regex("[0-9]{1,18}")

    fun parse(raw: String): Long? {
        val code = raw.trim()
        val numberText = when {
            code.startsWith("OYL:ID:", ignoreCase = true) -> code.substring(7).trim()
            code.startsWith("OYL:", ignoreCase = true) -> code.substring(4).trim()
            else -> code
        }
        if (!validNumber.matches(numberText)) return null
        return numberText.toLongOrNull()?.takeIf { it > 0L }
    }
}
