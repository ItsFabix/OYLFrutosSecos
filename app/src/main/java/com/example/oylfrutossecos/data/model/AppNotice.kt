package com.example.oylfrutossecos.data.model

enum class NoticeKind { ORDER_CREATED, ORDER_STATUS, STOCK_LOW, TEST }

data class AppNotice(
    val id: Long,
    val userId: Long,
    val kind: NoticeKind,
    val title: String,
    val message: String,
    val target: String,
    val createdAt: Long,
    val readAt: Long?
) {
    val isRead: Boolean get() = readAt != null
}
