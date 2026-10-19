package com.example.oylfrutossecos.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oylfrutossecos.data.model.Registration
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.data.model.UserRole
import com.example.oylfrutossecos.data.repository.AliasAlreadyUsedException
import com.example.oylfrutossecos.data.repository.AuthRepository
import com.example.oylfrutossecos.domain.AuthValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class LoginState(
    val alias: String = "",
    val password: String = "",
    val role: UserRole = UserRole.CLIENT,
    val errors: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val busy: Boolean = false
)

data class RegisterState(
    val form: Registration = Registration(),
    val errors: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val busy: Boolean = false,
    val completed: Boolean = false
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    var loginState by mutableStateOf(LoginState())
        private set
    var registerState by mutableStateOf(RegisterState())
        private set
    var user by mutableStateOf<User?>(null)
        private set
    var restoring by mutableStateOf(true)
        private set
    var sessionError by mutableStateOf<String?>(null)
        private set
    var loggingOut by mutableStateOf(false)
        private set

    init { restoreSession() }

    fun restoreSession() {
        restoring = true
        sessionError = null
        viewModelScope.launch {
            try {
                user = repository.currentUser()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                sessionError = "No se pudo abrir la sesión local. Intenta nuevamente."
            } finally {
                restoring = false
            }
        }
    }

    fun changeLogin(alias: String = loginState.alias, password: String = loginState.password,
                    role: UserRole = loginState.role) {
        if (loginState.busy) return
        loginState = loginState.copy(alias = alias, password = password, role = role,
            errors = emptyMap(), errorMessage = null, successMessage = null)
    }

    fun login() {
        if (loginState.busy) return
        val submitted = loginState
        val errors = AuthValidator.login(submitted.alias, submitted.password)
        loginState = submitted.copy(errors = errors, errorMessage = null, successMessage = null)
        if (errors.isNotEmpty()) return
        loginState = loginState.copy(busy = true)
        viewModelScope.launch {
            try {
                val found = repository.login(submitted.alias, submitted.password, submitted.role)
                if (found == null) {
                    loginState = loginState.copy(errorMessage = "Revisa el alias, la contraseña y el tipo de usuario.")
                } else {
                    user = found
                    loginState = LoginState()
                    registerState = RegisterState()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                loginState = loginState.copy(errorMessage = "No se pudo iniciar sesión. Intenta nuevamente.")
            } finally {
                loginState = loginState.copy(busy = false)
            }
        }
    }

    fun changeRegistration(form: Registration) {
        if (registerState.busy) return
        registerState = registerState.copy(form = form, errors = emptyMap(), errorMessage = null)
    }

    fun register() {
        if (registerState.busy) return
        val form = registerState.form
        val errors = AuthValidator.registration(form)
        registerState = registerState.copy(errors = errors, errorMessage = null)
        if (errors.isNotEmpty()) return
        registerState = registerState.copy(busy = true)
        viewModelScope.launch {
            try {
                repository.register(form)
                loginState = LoginState(alias = AuthValidator.normalizeAlias(form.alias),
                    successMessage = "Cuenta creada. Ahora puedes iniciar sesión.")
                registerState = RegisterState(completed = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: AliasAlreadyUsedException) {
                registerState = registerState.copy(errors = mapOf("alias" to "Este alias ya está registrado"))
            } catch (error: Exception) {
                registerState = registerState.copy(errorMessage = "No se pudo guardar la cuenta. Intenta nuevamente.")
            } finally {
                registerState = registerState.copy(busy = false)
            }
        }
    }

    fun consumeRegistration() { registerState = RegisterState() }

    fun clearForms() {
        if (!loginState.busy && !registerState.busy) {
            loginState = LoginState()
            registerState = RegisterState()
        }
    }

    fun logout() {
        if (loggingOut) return
        loggingOut = true
        sessionError = null
        viewModelScope.launch {
            try {
                repository.logout()
                user = null
                clearForms()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                sessionError = "No se pudo cerrar sesión. Intenta nuevamente."
            } finally {
                loggingOut = false
            }
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
    }
}
