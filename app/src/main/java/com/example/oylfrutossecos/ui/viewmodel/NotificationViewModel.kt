package com.example.oylfrutossecos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oylfrutossecos.data.model.AppNotice
import com.example.oylfrutossecos.data.model.NoticeKind
import com.example.oylfrutossecos.data.repository.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
    val items: List<AppNotice> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null
) {
    val unreadCount: Int get() = items.count { !it.isRead }
}

class NotificationViewModel(private val repository: NotificationRepository,
                            private val userId: Long) : ViewModel() {
    private val mutable = MutableStateFlow(NotificationUiState())
    val state = mutable.asStateFlow()
    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                val items = repository.list(userId)
                mutable.update { it.copy(items = items, loading = false, error = null) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutable.update { it.copy(loading = false, error = "No se pudieron cargar los avisos.") }
            }
        }
    }

    // Muestra aviso Android únicamente para una acción que acaba de realizar este usuario.
    fun announceIfNew(kind: NoticeKind, startedAt: Long, onFound: (AppNotice) -> Unit) {
        viewModelScope.launch {
            try {
                val latest = repository.list(userId).firstOrNull { notice ->
                    notice.kind == kind && notice.createdAt >= startedAt
                }
                if (latest != null) onFound(latest)
                load()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { load() }
        }
    }

    fun read(id: Long) {
        viewModelScope.launch {
            try {
                repository.markRead(userId, id)
                load()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutable.update { it.copy(error = "No se pudo marcar el aviso como leído.") }
            }
        }
    }

    fun readAll() {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                repository.markAllRead(userId)
                val items = repository.list(userId)
                mutable.update { it.copy(items = items, busy = false, error = null) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutable.update { it.copy(busy = false, error = "No se pudieron actualizar los avisos.") }
            }
        }
    }

    fun createTest(onCreated: (AppNotice) -> Boolean) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val notice = repository.createTest(userId)
                val notified = try { onCreated(notice) } catch (_: Exception) { false }
                val items = repository.list(userId)
                mutable.update { it.copy(items = items, busy = false,
                    message = if (notified) "Aviso de comprobación enviado."
                        else "Aviso guardado. Para verlo también fuera de la app, permite las notificaciones.") }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutable.update { it.copy(busy = false, error = "No se pudo crear el aviso. Intenta nuevamente.") }
            }
        }
    }

    class Factory(private val repository: NotificationRepository,
                  private val userId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NotificationViewModel::class.java))
            return NotificationViewModel(repository, userId) as T
        }
    }
}
