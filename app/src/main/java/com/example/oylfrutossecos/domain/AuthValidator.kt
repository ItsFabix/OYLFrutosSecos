package com.example.oylfrutossecos.domain

import com.example.oylfrutossecos.data.model.Registration
import java.util.Locale

// Reglas de la aplicación independientes de Compose y de Android.
object AuthValidator {
    fun normalizeAlias(value: String): String = value.trim().lowercase(Locale.ROOT)

    fun login(alias: String, password: String): Map<String, String> = buildMap {
        if (alias.isBlank()) put("alias", "Ingresa tu alias")
        if (password.isBlank()) put("password", "Ingresa tu contraseña")
    }

    fun registration(form: Registration): Map<String, String> = buildMap {
        if (form.name.trim().length !in 2..80) {
            put("name", "Ingresa un nombre de 2 a 80 caracteres")
        }
        val alias = normalizeAlias(form.alias)
        if (!Regex("[a-z0-9_]{3,20}").matches(alias)) {
            put("alias", "Usa de 3 a 20 letras sin tilde, números o guion bajo")
        } else if (alias == "admin") {
            put("alias", "Este alias está reservado")
        }
        if (form.email.trim().length > 120 ||
            !Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(form.email.trim())) {
            put("email", "Ingresa un correo electrónico válido")
        }
        val phone = form.phone.replace(" ", "").replace("-", "")
        if (!Regex("(?:\\+56)?9[0-9]{8}").matches(phone)) {
            put("phone", "Usa 9 dígitos comenzando por 9, o +56 seguido de ellos")
        }
        if (form.address.trim().length !in 5..160) {
            put("address", "Ingresa una dirección de 5 a 160 caracteres")
        }
        if (form.password.length !in 8..128 ||
            form.password.none { it.isLetter() } ||
            form.password.none { it.isDigit() }) {
            put("password", "Usa de 8 a 128 caracteres, con letras y números")
        }
        if (form.confirmation.isBlank() || form.confirmation != form.password) {
            put("confirmation", "Las contraseñas deben coincidir")
        }
    }
}
