package com.example.oylfrutossecos.data.model

enum class UserRole(val label: String) {
    CLIENT("Cliente"), ADMIN("Administrador")
}

data class User(
    val id: Long,
    val name: String,
    val alias: String,
    val email: String,
    val phone: String,
    val address: String,
    val role: UserRole
)

data class Registration(
    val name: String = "",
    val alias: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val password: String = "",
    val confirmation: String = ""
)
