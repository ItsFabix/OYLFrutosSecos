package com.example.oylfrutossecos.data.model

/** Solo campos personales editables; no acepta identificador, rol ni credenciales. */
data class ProfileDraft(val name: String = "", val phone: String = "", val address: String = "") {
    companion object {
        fun from(user: User) = ProfileDraft(user.name, user.phone, user.address)
    }
}
