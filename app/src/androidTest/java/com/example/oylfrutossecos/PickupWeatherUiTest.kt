package com.example.oylfrutossecos

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.OpenMeteoClient
import com.example.oylfrutossecos.data.repository.*
import com.example.oylfrutossecos.ui.screens.PickupMapScreen
import com.example.oylfrutossecos.ui.theme.OYLFrutosSecosTheme
import com.example.oylfrutossecos.ui.viewmodel.PickupWeatherState
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class PickupWeatherUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val point = DemoPickupPoints.all.first()
    private fun render(state: PickupWeatherState, onRetry: () -> Unit = {}, onPoint: (PickupPoint) -> Unit = {}) {
        compose.setContent { OYLFrutosSecosTheme { Surface {
            Box(Modifier.width(320.dp).height(640.dp)) {
                PickupMapScreen(state, onPoint, onRetry, onBack = {})
            }
        } } }
    }

    private fun awaitMapCenter(point: PickupPoint) {
        // osmdroid updates its native projection outside Compose's frame clock.
        compose.waitUntil(10000) {
            var centered = false
            compose.runOnIdle {
                val map = compose.activity.findViewById<android.view.View>(android.R.id.content)
                    .findViewWithTag<org.osmdroid.views.MapView>("pickup-map")
                centered = kotlin.math.abs(point.latitude - map.mapCenter.latitude) < 0.0001 &&
                    kotlin.math.abs(point.longitude - map.mapCenter.longitude) < 0.0001
            }
            centered
        }
    }

    @Test fun loadingDoesNotDisableMapOrPointSelectionAt320dp() {
        var selected: PickupPoint? = null
        render(PickupWeatherState(point.id, loading = true), onPoint = { selected = it })
        compose.onNodeWithText("Ver todos").assertIsSelected()
        compose.runOnIdle { assertNull(selected) }
        compose.onNodeWithText("Centro").performClick()
        compose.onNodeWithText("Consultando el clima…").assertIsDisplayed()
        compose.onNodeWithText("Cómo llegar").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Centrar mapa").assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithText("Ñuñoa").assertIsNotSelected().performClick().assertIsSelected()
        awaitMapCenter(DemoPickupPoints.all.last())
        compose.runOnIdle { assertEquals(DemoPickupPoints.all.last(), selected) }
        compose.onNodeWithText("Referencia: Plaza Ñuñoa, Santiago").assertIsDisplayed()
        compose.runOnIdle {
            val map = compose.activity.findViewById<android.view.View>(android.R.id.content)
                .findViewWithTag<org.osmdroid.views.MapView>("pickup-map")
            assertEquals(selected!!.latitude, map.mapCenter.latitude, 0.0001)
            assertEquals(selected!!.longitude, map.mapCenter.longitude, 0.0001)
            val markers = map.overlays.filterIsInstance<org.osmdroid.views.overlay.Marker>()
            assertEquals(2, markers.size)
            assertEquals(1f, markers.single { it.title == selected!!.title.removeSuffix(" (demo)") }.alpha)
            assertTrue(markers.single { it.title != selected!!.title.removeSuffix(" (demo)") }.alpha < 1f)
        }
        compose.onNodeWithText("Centro").performClick().assertIsSelected()
        awaitMapCenter(point)
        compose.runOnIdle {
            assertEquals(point, selected)
            val map = compose.activity.findViewById<android.view.View>(android.R.id.content)
                .findViewWithTag<org.osmdroid.views.MapView>("pickup-map")
            assertEquals(point.latitude, map.mapCenter.latitude, 0.0001)
            assertEquals(point.longitude, map.mapCenter.longitude, 0.0001)
        }
    }

    @Test fun failureOffersRetryAndKeepsMapActionsAvailable() {
        var retries = 0
        render(PickupWeatherState(point.id, error = "No se pudo conectar con el servicio del clima."), { retries++ })
        compose.onNodeWithText("Centro").performClick()
        compose.onNodeWithText("El mapa sigue disponible.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar clima").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        compose.onNodeWithText("Centrar mapa").assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithText("Cómo llegar").assertIsEnabled()
    }

    @Test fun successShowsTemperatureSpanishConditionAndSourceAt320dp() {
        render(PickupWeatherState(point.id, weather = PickupWeather(21.5, 63, LocalDateTime.parse("2026-11-23T11:30"))))
        compose.onNodeWithText("Centro").performClick()
        compose.onNodeWithText("21,5 °C · Lluvia moderada").assertIsDisplayed()
        compose.onNodeWithText("Clima · Open-Meteo").assertIsDisplayed()
        compose.onNodeWithText("Dato del 23/11 11:30 · hora local").assertIsDisplayed()
        compose.onNodeWithText("Centrar mapa").assertIsDisplayed()
    }

    @Test fun optionalLiveApiUsesRetrofitFromEmulator() = runBlocking {
        assumeTrue("Sonda opcional de Internet", InstrumentationRegistry.getArguments().getString("oylWeatherLiveProbe") == "true")
        val weather = PickupWeatherRepository(OpenMeteoClient.service).current(point)
        assertTrue(weather.temperatureCelsius.isFinite())
        assertTrue(weather.weatherCode >= 0)
    }
}
