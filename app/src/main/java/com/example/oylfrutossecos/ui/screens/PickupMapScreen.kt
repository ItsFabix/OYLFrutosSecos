package com.example.oylfrutossecos.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.Alignment
import com.example.oylfrutossecos.ui.components.*
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clipToBounds
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
@OptIn(ExperimentalLayoutApi::class)
fun PickupMapScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val points = DemoPickupPoints.all
    var query by remember { mutableStateOf("") }
    val visiblePoints = points.filter { it.title.contains(query, true) || it.reference.contains(query, true) }
    var selectedId by remember { mutableStateOf(points.first().id) }
    var externalError by remember { mutableStateOf<String?>(null) }
    val point = points.first { it.id == selectedId }
    val keyboardVisible = WindowInsets.isImeVisible

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

    LaunchedEffect(selectedId, mapView, query) {
        val focus = GeoPoint(point.latitude, point.longitude)
        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(focus)
        mapView.overlays.clear()
        visiblePoints.forEach { item ->
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(item.latitude, item.longitude)
                title = item.title
                snippet = item.reference
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ -> selectedId = item.id; true }
            })
        }
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

    Box(Modifier.fillMaxSize().imePadding().clipToBounds().testTag("map-screen")) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(horizontal = 10.dp)) {
            ReferenceTitle("Puntos de retiro", onBack)
            OutlinedTextField(query, { input ->
                query = input
                points.firstOrNull { it.title.contains(input, true) || it.reference.contains(input, true) }?.let { selectedId = it.id }
            }, label = { Text("Buscar puntos de retiro") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                visiblePoints.forEach { item ->
                    OylFilterChip(selected = item.id == selectedId, onClick = { selectedId = item.id; externalError = null },
                        label = { Text(item.title.removeSuffix(" (demo)"), style = MaterialTheme.typography.labelSmall) })
                }
            }
            if (visiblePoints.isEmpty()) Text("Sin puntos coincidentes", style = MaterialTheme.typography.bodySmall)
        }
        if (!keyboardVisible) Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp),
            shape = MaterialTheme.shapes.medium, shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(point.title.removeSuffix(" (demo)"), style = MaterialTheme.typography.titleSmall)
                Text(point.reference, style = MaterialTheme.typography.bodySmall)
                Text(point.schedule.replace("Horario ficticio:", "Horario referencial:"),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { openExternal(DemoPickupPoints.directionsUrl(point)) }, modifier = Modifier.weight(1f)) { Text("Cómo llegar") }
                    OutlinedButton(onClick = {
                        mapView.controller.setZoom(16.0)
                        mapView.controller.animateTo(GeoPoint(point.latitude, point.longitude))
                    }, modifier = Modifier.weight(1f)) { Text("Centrar mapa") }
                }
                TextButton(onClick = { openExternal(DemoPickupPoints.browserMapUrl(point)) }) { Text("Abrir mapa en el navegador") }
                externalError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text("Puntos y horarios referenciales. © OpenStreetMap contributors. Requiere Internet.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
