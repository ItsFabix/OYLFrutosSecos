package com.example.oylfrutossecos.domain

import com.example.oylfrutossecos.data.model.ProfileDraft

object ProfileValidator {
    fun validate(draft: ProfileDraft): Map<String, String> = buildMap {
        if (draft.name.trim().length !in 2..80) put("name", "Ingresa un nombre de 2 a 80 caracteres.")
        val phone = draft.phone.replace(" ", "").replace("-", "")
        if (!Regex("(?:\\+56)?9[0-9]{8}").matches(phone))
            put("phone", "Usa 9 dígitos comenzando por 9, o +56 seguido de ellos.")
        if (draft.address.trim().length !in 5..160) put("address", "Ingresa una dirección de 5 a 160 caracteres.")
    }
}
