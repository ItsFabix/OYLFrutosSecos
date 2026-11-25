package com.example.oylfrutossecos.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oylfrutossecos.data.remote.OylRestService
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import retrofit2.HttpException

/** Solo memoria: no SavedStateHandle, preferencias, SQLite ni BuildConfig. */
class AdminSessionViewModel(private val operationScope: CoroutineScope? = null) : ViewModel() {
    @Volatile private var credential = ""
    private var validationJob: Job? = null
    private var generation = 0
    var configured by mutableStateOf(false)
        private set
    var validating by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    fun key(): String = credential
    fun reject(rejectedCredential: String) {
        // A late response for a previous key must not disconnect a replacement session.
        if (configured && credential == rejectedCredential) clear()
    }
    fun connect(value: String, api: OylRestService, onConnected: () -> Unit) {
        if (validating) return
        val attempt = ++generation
        credential = ""
        configured = false
        error = null
        if (value.length < 16) {
            error = "La clave administrativa debe tener al menos 16 caracteres."
            return
        }
        validating = true
        validationJob = (operationScope ?: viewModelScope).launch {
            try {
                // Solo lecturas: no se declara acceso válido por haber escrito una clave.
                api.adminOrders(value)
                api.inventoryMovements(value)
                currentCoroutineContext().ensureActive()
                if (attempt != generation) return@launch
                credential = value
                configured = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (attempt != generation) return@launch
                // No mostrar cuerpos HTTP, mensajes arbitrarios ni la credencial.
                error = when (failure) {
                    is HttpException -> when (failure.code()) {
                        401, 403 -> "La clave administrativa no es válida. Revísala e inténtalo nuevamente."
                        404 -> "El acceso administrativo no está disponible. Contacta a la administración."
                        else -> "No pudimos validar el acceso. Inténtalo más tarde."
                    }
                    is IOException -> "No pudimos establecer una conexión. Revisa tu conexión e inténtalo nuevamente."
                    else -> "No pudimos validar el acceso. Inténtalo nuevamente."
                }
            } finally { if (attempt == generation) validating = false }
            if (attempt == generation && configured) onConnected()
        }
    }
    fun clear() {
        generation++
        validationJob?.cancel()
        validationJob = null
        credential = ""
        configured = false
        validating = false
        error = null
    }
    override fun onCleared() { clear() }
}
