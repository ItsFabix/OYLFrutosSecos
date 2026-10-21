package com.example.oylfrutossecos.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.oylfrutossecos.data.repository.DemoPickupPoints
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Mapa nativo con osmdroid (sin WebView ni clave de Google Maps).
 * Usa puntos FICTICIOS de la demostración académica; no representa sucursales reales.
 * OpenStreetMap se obtiene por Internet; los mosaicos se almacenan en caché por la librería.
 */
@Composable
fun PickupMapScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val points = DemoPickupPoints.all
    var selectedId by remember { mutableStateOf(points.first().id) }
    var externalError by remember { mutableStateOf<String?>(null) }
    val point = points.first { it.id == selectedId }

    // Conserva MapView entre recomposiciones y configura osmdroid antes de crearla.
    val mapView = remember(context) {
        Configuration.getInstance().load(
            context.applicationContext,
            context.applicationContext.getSharedPreferences("oyl_osmdroid", Context.MODE_PRIVATE)
        )
        // Política OSM: User-Agent estable, específico del proyecto y no el genérico com.example.*.
        Configuration.getInstance().userAgentValue =
            "OYLFrutosSecos/1.0 (+https://github.com/ItsFabix/OYLFrutosSecos)"
        MapView(context).apply {
            // URL canónica HTTPS exigida por la política de mosaicos de OSM.
            // Caché separada de MAPNIK para evitar reutilizar respuestas 403 anteriores.
            // NO hacemos descargas masivas ni anulamos el caché de osmdroid.
            setTileSource(
                XYTileSource(
                    "OYL_OSM_Standard", 1, 19, 256, ".png",
                    arrayOf("https://tile.openstreetmap.org/")
                )
            )
            setMultiTouchControls(true)
            setBuiltInZoomControls(true)
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(point.latitude, point.longitude))
            // El mapa recibe el gesto, no el scroll vertical de la pantalla principal.
            setOnTouchListener { view, _ ->
                view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    // Desconectar recursos y peticiones cuando la pantalla sale de Compose.
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(selectedId, mapView) {
        val focus = GeoPoint(point.latitude, point.longitude)
        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(focus)
        mapView.overlays.clear()
        mapView.overlays.add(
            Marker(mapView).apply {
                position = focus
                title = point.title
                snippet = point.reference
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
        )
        mapView.invalidate()
    }

    fun openExternal(url: String) {
        externalError = null
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            externalError = "No se encontró una aplicación para abrir el mapa."
        } catch (_: SecurityException) {
            externalError = "Android no permitió abrir el mapa externo."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Volver") }
        Text("Mapa y puntos de retiro", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Ubicaciones y horarios FICTICIOS para probar el proyecto. No son sucursales reales de OYL.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        Text("Seleccionar punto", style = MaterialTheme.typography.titleMedium)
        points.forEach { item ->
            FilterChip(
                selected = item.id == selectedId,
                onClick = {
                    selectedId = item.id
                    externalError = null
                },
                label = { Text(item.title) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(point.title, style = MaterialTheme.typography.titleMedium)
                Text(point.reference)
                Text(point.schedule, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("Mapa interactivo (requiere Internet)", style = MaterialTheme.typography.titleMedium)
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxWidth().height(310.dp)
        )
        Text(
            "Arrastra para mover el mapa, pellizca para acercar y toca el marcador. © OpenStreetMap contributors.",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Si el servidor no muestra calles, utiliza «Abrir mapa en el navegador». La disponibilidad de mapas depende de OpenStreetMap.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(
            onClick = {
                mapView.controller.setZoom(16.0)
                mapView.controller.animateTo(GeoPoint(point.latitude, point.longitude))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Centrar mapa en el punto")
        }
        Button(
            onClick = { openExternal(DemoPickupPoints.directionsUrl(point)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Como llegar (Google Maps)") }
        OutlinedButton(
            onClick = { openExternal(DemoPickupPoints.browserMapUrl(point)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Abrir mapa en el navegador") }
        if (externalError != null) {
            Text(externalError.orEmpty(), color = MaterialTheme.colorScheme.error)
        }
        Text(
            "Esta es una demostracion local. Las ubicaciones reales deben validarse antes de usarlas en produccion.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
