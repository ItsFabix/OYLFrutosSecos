package com.example.oylfrutossecos

import android.graphics.Bitmap
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.model.PickupWeather
import com.example.oylfrutossecos.data.repository.DemoPickupPoints
import com.example.oylfrutossecos.ui.screens.PickupMapScreen
import com.example.oylfrutossecos.ui.theme.OYLFrutosSecosTheme
import com.example.oylfrutossecos.ui.viewmodel.PickupWeatherState
import java.io.File
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Real native layout/projection at 320 dp; no business repositories or user database. */
class PickupMapOverviewUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val points = DemoPickupPoints.all
    private fun map(): MapView = compose.activity.findViewById<View>(android.R.id.content).findViewWithTag("pickup-map")
    private fun markers() = map().overlays.filterIsInstance<Marker>()

    private fun awaitOverview() {
        compose.onNodeWithText("Ver todos").assertIsSelected()
        var diagnostic = "Map not measured"
        try { compose.waitUntil(10000) {
            var fitted = false
            compose.runOnIdle {
                val map = map()
                val margin = 20 * map.resources.displayMetrics.density
                diagnostic = "viewport=${map.width}x${map.height}, zoom=${map.zoomLevelDouble}, markers=" + markers().map {
                    map.projection.toPixels(it.position, null).toString()
                }
                fitted = map.width > 0 && map.height > 0 && markers().size == points.size && points.all {
                    val pixel = map.projection.toPixels(GeoPoint(it.latitude, it.longitude), null)
                    pixel.x >= margin && pixel.x <= map.width - margin &&
                        pixel.y >= margin && pixel.y <= map.height - margin
                }
            }
            fitted
        } } catch (timeout: ComposeTimeoutException) { throw AssertionError(diagnostic, timeout) }
        compose.runOnIdle { assertTrue(markers().all { it.alpha == 1f }) }
    }

    private fun awaitLocal(index: Int) {
        val point = points[index]
        compose.waitUntil(10000) {
            var centered = false
            compose.runOnIdle {
                centered = kotlin.math.abs(map().mapCenter.latitude - point.latitude) < 0.0001 &&
                    kotlin.math.abs(map().mapCenter.longitude - point.longitude) < 0.0001
            }
            centered
        }
        compose.runOnIdle {
            val selected = markers().single { it.title == point.title }
            val other = markers().single { it.title != point.title }
            assertEquals(1f, selected.alpha)
            assertTrue(other.alpha < selected.alpha)
            assertTrue(selected.icon.intrinsicWidth > other.icon.intrinsicWidth)
        }
        compose.onNodeWithText(point.reference).assertIsDisplayed()
    }

    private fun capture(name: String) {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "map-overview-qa")
        check(directory.exists() || directory.mkdirs())
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun initialOverviewSelectsBothLocalsAndReturnsToAllWithVisibleMarkers() {
        var weatherRequests = 0
        compose.setContent { OYLFrutosSecosTheme { Surface {
            Box(Modifier.size(320.dp, 640.dp)) {
                PickupMapScreen(onWeatherPoint = { weatherRequests++ }) {}
            }
        } } }
        capture("clima-vista-general-320dp")
        awaitOverview()
        compose.onNodeWithText("Vista general · 2 locales").assertIsDisplayed()
        compose.onNodeWithText("Centro").assertIsNotSelected()
        compose.onNodeWithText("Ñuñoa").assertIsNotSelected()
        compose.onNodeWithText("Cómo llegar").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, weatherRequests) }
        capture("vista-general-320dp")
        compose.onNodeWithText("Centro").performClick().assertIsSelected()
        awaitLocal(0)
        compose.onNodeWithText("Cómo llegar").assertIsEnabled()
        compose.onNodeWithText("Ñuñoa").performClick().assertIsSelected()
        awaitLocal(1)
        capture("nunoa-320dp")
        compose.onNodeWithText("Ver todos").performClick()
        awaitOverview()
        compose.runOnIdle { assertEquals(2, weatherRequests) }
        // An explicit tap on the active overview must also recover a manually moved camera.
        compose.runOnIdle { map().controller.setZoom(map().zoomLevelDouble + 2) }
        compose.onNodeWithText("Ver todos").performClick()
        awaitOverview()
        compose.runOnIdle { assertEquals(2, weatherRequests) }
    }

    @Test fun framingWaitsForNonzeroNativeLayoutAndUsesAvailableViewport() {
        val width = mutableStateOf(320.dp)
        val height = mutableStateOf(640.dp)
        compose.setContent { OYLFrutosSecosTheme {
            Box(Modifier.size(width.value, height.value)) { PickupMapScreen {} }
        } }
        awaitOverview()
        compose.runOnIdle { assertTrue(map().isLaidOut); assertTrue(map().width > 0); assertTrue(map().height > 0) }
        val smallZoom = compose.runOnIdle { map().zoomLevelDouble }
        // A new visit in a larger viewport derives a different zoom from the same locations.
        compose.runOnIdle { width.value = 390.dp; height.value = 750.dp }
        compose.onNodeWithText("Ver todos").performClick()
        awaitOverview()
        compose.runOnIdle { assertTrue(map().zoomLevelDouble > smallZoom) }
    }

    @Test fun weatherAndUnrelatedRecompositionsPreserveUserCameraInLocalAndOverview() {
        val weather = mutableStateOf(PickupWeatherState())
        val tick = mutableIntStateOf(0)
        var requests = 0
        compose.setContent { OYLFrutosSecosTheme { Surface {
            Column(Modifier.size(320.dp, 640.dp)) {
                Text("Actualización ${tick.intValue}")
                Box(Modifier.weight(1f)) {
                    PickupMapScreen(weather.value, onWeatherPoint = { requests++ }) {}
                }
            }
        } } }
        awaitOverview()
        fun moveAndCapture(): Triple<Double, Double, Double> {
            compose.runOnIdle {
                val map = map()
                map.controller.setZoom(map.zoomLevelDouble + 0.5)
                map.controller.setCenter(GeoPoint(map.mapCenter.latitude + 0.001, map.mapCenter.longitude + 0.001))
            }
            compose.waitForIdle()
            return compose.runOnIdle { Triple(map().zoomLevelDouble, map().mapCenter.latitude, map().mapCenter.longitude) }
        }
        fun assertCamera(camera: Triple<Double, Double, Double>) {
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(camera.first, map().zoomLevelDouble, 0.00001)
                assertEquals(camera.second, map().mapCenter.latitude, 0.0001)
                assertEquals(camera.third, map().mapCenter.longitude, 0.0001)
            }
        }
        val overviewCamera = moveAndCapture()
        compose.runOnIdle {
            weather.value = PickupWeatherState(points[0].id, loading = true)
            tick.intValue++
        }
        assertCamera(overviewCamera)
        compose.onNodeWithText("Ver todos").assertIsSelected()
        compose.runOnIdle { assertEquals(0, requests) }
        compose.onNodeWithText("Centro").performClick()
        awaitLocal(0)
        val localCamera = moveAndCapture()
        compose.runOnIdle {
            weather.value = PickupWeatherState(points[0].id,
                weather = PickupWeather(21.5, 0, LocalDateTime.parse("2026-11-23T11:30")))
            tick.intValue++
        }
        compose.onNodeWithText("21,5 °C · Despejado").assertIsDisplayed()
        assertCamera(localCamera)
        compose.onNodeWithText("Ver todos").performClick()
        awaitOverview()
        val returnedCamera = moveAndCapture()
        compose.runOnIdle {
            weather.value = PickupWeatherState(points[0].id, error = "Respuesta tardía")
            tick.intValue++
        }
        assertCamera(returnedCamera)
        compose.onNodeWithText("Ver todos").assertIsSelected()
        compose.onNodeWithText("Respuesta tardía").assertDoesNotExist()
    }
}
