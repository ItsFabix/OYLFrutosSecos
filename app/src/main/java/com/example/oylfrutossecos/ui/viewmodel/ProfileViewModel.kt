package com.example.oylfrutossecos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.AuthRepository
import com.example.oylfrutossecos.domain.ProfileValidator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileState(val draft: ProfileDraft, val errors: Map<String, String> = emptyMap(),
    val busy: Boolean = false, val error: String? = null, val saved: Boolean = false)

class ProfileViewModel(
    private val owner: User,
    private val persist: suspend (Long, ProfileDraft) -> User,
    private val operationScope: CoroutineScope? = null
) : ViewModel() {
    private val mutable = MutableStateFlow(ProfileState(ProfileDraft.from(owner)))
    val state = mutable.asStateFlow()

    fun change(draft: ProfileDraft) {
        val current = mutable.value
        if (current.busy || current.saved) return
        val changed = buildSet {
            if (draft.name != current.draft.name) add("name")
            if (draft.phone != current.draft.phone) add("phone")
            if (draft.address != current.draft.address) add("address")
        }
        mutable.value = current.copy(draft = draft, errors = current.errors - changed, error = null)
    }

    fun save(onSaved: (User) -> Unit) {
        val current = mutable.value
        if (current.busy || current.saved) return
        val errors = ProfileValidator.validate(current.draft)
        mutable.value = current.copy(errors = errors, error = null)
        if (errors.isNotEmpty()) return
        mutable.value = mutable.value.copy(busy = true)
        (operationScope ?: viewModelScope).launch {
            try {
                val updated = persist(owner.id, current.draft)
                check(updated.id == owner.id && updated.alias == owner.alias && updated.role == owner.role && updated.email == owner.email)
                mutable.value = mutable.value.copy(busy = false, saved = true)
                onSaved(updated)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: IllegalStateException) {
                mutable.value = mutable.value.copy(busy = false, error = "La sesión cambió o no se pudo guardar el perfil. Vuelve a Cuenta y reintenta.")
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(busy = false, error = "No se pudo guardar tu perfil. Tus cambios siguen en el formulario.")
            }
        }
    }

    class Factory(private val user: User, private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
            return ProfileViewModel(user, repository::updateOwnProfile) as T
        }
    }
}
