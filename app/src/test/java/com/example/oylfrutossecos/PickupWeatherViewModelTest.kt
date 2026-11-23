package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.remote.*
import com.example.oylfrutossecos.data.repository.*
import com.example.oylfrutossecos.domain.WeatherConditions
import com.example.oylfrutossecos.ui.viewmodel.PickupWeatherViewModel
import java.io.IOException
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class PickupWeatherViewModelTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val points = DemoPickupPoints.all
    private fun response(temperature: Double = 22.0) = OpenMeteoResponse(OpenMeteoCurrent("2026-11-23T11:30", temperature, 0))
    private class FakeService(val fetch: suspend (Double, Double) -> OpenMeteoResponse) : OpenMeteoService {
        var calls = 0
        override suspend fun currentWeather(latitude: Double, longitude: Double, current: String,
            temperatureUnit: String, timezone: String): OpenMeteoResponse {
            calls++
            return fetch(latitude, longitude)
        }
    }
    @After fun close() { scope.cancel() }

    @Test fun loadingThenSuccessAndRepeatedSelectionDoesNotRequestAgain() {
        val gate = CompletableDeferred<OpenMeteoResponse>()
        val api = FakeService { _, _ -> gate.await() }
        val vm = PickupWeatherViewModel(PickupWeatherRepository(api), scope)
        vm.select(points.first())
        assertTrue(vm.state.value.loading)
        repeat(5) { vm.select(points.first()); vm.retry() }
        assertEquals(1, api.calls)
        gate.complete(response())
        assertFalse(vm.state.value.loading)
        assertEquals(22.0, vm.state.value.weather!!.temperatureCelsius, 0.001)
        assertNull(vm.state.value.error)
    }

    @Test fun networkErrorAllowsExplicitRetryWithoutAutomaticRequestLoop() {
        var offline = true
        val api = FakeService { _, _ -> if (offline) throw IOException("offline") else response() }
        val vm = PickupWeatherViewModel(PickupWeatherRepository(api), scope)
        vm.select(points.first())
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.weather)
        assertTrue(vm.state.value.error!!.contains("No se pudo conectar"))
        repeat(5) { vm.select(points.first()) }
        assertEquals(1, api.calls)
        offline = false
        vm.retry()
        assertNotNull(vm.state.value.weather)
        assertNull(vm.state.value.error)
        assertEquals(2, api.calls)
    }

    @Test fun switchingPointsShowsTheirOwnWeatherAndReusesPreviousCache() {
        val api = FakeService { lat, _ -> response(if (lat == points.first().latitude) 20.0 else 19.0) }
        val vm = PickupWeatherViewModel(PickupWeatherRepository(api), scope)
        vm.select(points.first())
        vm.select(points.last())
        assertEquals(points.last().id, vm.state.value.pointId)
        assertEquals(19.0, vm.state.value.weather!!.temperatureCelsius, 0.001)
        vm.select(points.first())
        assertEquals(20.0, vm.state.value.weather!!.temperatureCelsius, 0.001)
        assertEquals(2, api.calls)
    }

    @Test fun lateResponseFromPreviousSelectionCannotReplaceSelectedPoint() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val api = FakeService { lat, _ ->
            if (lat == points.first().latitude) withContext(NonCancellable) { gate.await() }
            response(if (lat == points.first().latitude) 20.0 else 19.0)
        }
        val vm = PickupWeatherViewModel(PickupWeatherRepository(api), scope)
        vm.select(points.first())
        vm.select(points.last())
        assertEquals(points.last().id, vm.state.value.pointId)
        gate.complete(Unit)
        yield()
        assertEquals(points.last().id, vm.state.value.pointId)
        assertEquals(19.0, vm.state.value.weather!!.temperatureCelsius, 0.001)
    }

    @Test fun wmoDescriptionsAreSpanishAndUnknownCodeDoesNotInventConditions() {
        assertEquals("Despejado", WeatherConditions.description(0))
        assertEquals("Lluvia moderada", WeatherConditions.description(63))
        assertEquals("Tormenta eléctrica intensa", WeatherConditions.description(97))
        assertEquals("Condición no disponible", WeatherConditions.description(999))
    }
}
