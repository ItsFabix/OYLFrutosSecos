package com.example.oylfrutossecos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oylfrutossecos.data.model.PickupPoint
import com.example.oylfrutossecos.data.model.PickupWeather
import com.example.oylfrutossecos.data.repository.PickupWeatherRepository
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException

data class PickupWeatherState(
    val pointId: String? = null,
    val loading: Boolean = false,
    val weather: PickupWeather? = null,
    val error: String? = null
)

class PickupWeatherViewModel(
    private val repository: PickupWeatherRepository,
    private val operationScope: CoroutineScope? = null
) : ViewModel() {
    private val mutable = MutableStateFlow(PickupWeatherState())
    val state = mutable.asStateFlow()
    private var selected: PickupPoint? = null
    private var request: Job? = null

    fun select(point: PickupPoint) {
        if (selected == point) return
        selected = point
        load(point)
    }

    fun retry() {
        if (mutable.value.loading) return
        selected?.let(::load)
    }

    private fun load(point: PickupPoint) {
        request?.cancel()
        mutable.value = PickupWeatherState(point.id, loading = true)
        request = (operationScope ?: viewModelScope).launch {
            try {
                val weather = repository.current(point)
                currentCoroutineContext().ensureActive()
                mutable.value = PickupWeatherState(point.id, weather = weather)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) {
                currentCoroutineContext().ensureActive()
                val message = when (failure) {
                    is IOException -> "No se pudo conectar con el servicio del clima."
                    is HttpException -> if (failure.code() == 429) "El servicio del clima está ocupado. Intenta más tarde."
                        else "El servicio del clima no está disponible."
                    else -> "El servicio no entregó información meteorológica válida."
                }
                mutable.value = PickupWeatherState(point.id, error = message)
            }
        }
    }

    class Factory(private val repository: PickupWeatherRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PickupWeatherViewModel::class.java))
            return PickupWeatherViewModel(repository) as T
        }
    }
}
