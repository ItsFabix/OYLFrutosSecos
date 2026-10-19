package com.example.oylfrutossecos.data.model

// Los pedidos del Paso 5 son DEMOSTRACIONES LOCALES, no ventas procesadas en servidor.
enum class OrderStatus(val displayName: String) {
    PENDING("Pendiente de validación"),
    CONFIRMED("Confirmado"),
    PREPARED("Preparado"),
    DELIVERED("Entregado");

    fun next(): OrderStatus? = when (this) {
        PENDING -> CONFIRMED
        CONFIRMED -> PREPARED
        PREPARED -> DELIVERED
        DELIVERED -> null
    }
}

enum class DeliveryMethod(val displayName: String) {
    PICKUP("Retiro en tienda"), DELIVERY("Entrega a domicilio")
}

data class OrderLine(
    val productId: Long,
    val name: String,
    val quantity: Int,
    val priceClp: Int
) {
    val subtotalClp: Long get() = priceClp.toLong() * quantity
}

data class OrderSummary(
    val id: Long,
    val userId: Long,
    val customerName: String,
    val createdAt: Long,
    val status: OrderStatus,
    val delivery: DeliveryMethod,
    val contactPhone: String,
    val contactAddress: String,
    val totalClp: Long,
    val lines: List<OrderLine>
)
