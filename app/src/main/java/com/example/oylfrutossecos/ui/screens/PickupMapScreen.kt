package com.example.oylfrutossecos.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import com.example.oylfrutossecos.ui.components.*
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.oylfrutossecos.data.repository.DemoPickupPoints
import com.example.oylfrutossecos.data.model.PickupPoint
import com.example.oylfrutossecos.ui.viewmodel.PickupWeatherState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlinx.coroutines.flow.first

/**
 * Mapa nativo con osmdroid (sin WebView ni clave de Google Maps).
 * Usa puntos FICTICIOS de la demostración académica; no representa sucursales reales.
 * OpenStreetMap se obtiene por Internet; los mosaicos se almacenan en caché por la librería.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PickupMapScreen(weatherState: PickupWeatherState = PickupWeatherState(),
    onWeatherPoint: (PickupPoint) -> Unit = {}, onRetryWeather: () -> Unit = {}, onBack: () -> Unit,
    points: List<PickupPoint> = DemoPickupPoints.all) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val visiblePoints = points.filter { it.title.contains(query, true) || it.reference.contains(query, true) }
    // Every new visit starts in overview; weather never owns the selection or camera.
    var selectedId by remember { mutableStateOf<String?>(null) }
    var frameRequest by remember { mutableIntStateOf(0) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    var externalError by remember { mutableStateOf<String?>(null) }
    val point = points.firstOrNull { it.id == selectedId }
    fun selectLocal(id: String?) {
        selectedId = id
        query = ""
        externalError = null
        frameRequest++ // Explicit taps also recenter an already selected local / overview.
    }
    val keyboardVisible = WindowInsets.isImeVisible
    val primary = MaterialTheme.colorScheme.primary.toArgb()
    val cream = MaterialTheme.colorScheme.surfaceContainerLowest.toArgb()
    val markers = remember(context, primary, cream) {
        listOf(true, false).associateWith { selected ->
            val size = (if (selected) 40 else 30) * context.resources.displayMetrics.density
            val bitmap = Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            Canvas(bitmap).apply {
                paint.color = primary
                drawCircle(size / 2, size / 2, size / 2, paint)
                paint.color = if (selected) primary else cream
                drawCircle(size / 2, size / 2, size / 2 - 3 * context.resources.displayMetrics.density, paint)
                paint.color = if (selected) cream else primary
                drawCircle(size / 2, size / 2, size / 6, paint)
            }
            BitmapDrawable(context.resources, bitmap)
        }
    }
    LaunchedEffect(point) { if (point != null) onWeatherPoint(point) }

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
            tag = "pickup-map"
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

    LaunchedEffect(selectedId, mapView, query, markers, points) {
        mapView.overlays.clear()
        visiblePoints.forEach { item ->
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(item.latitude, item.longitude)
                title = item.title.removeSuffix(" (demo)")
                snippet = item.reference
                icon = markers.getValue(item.id == selectedId)
                alpha = if (selectedId == null || item.id == selectedId) 1f else 0.65f
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ -> selectLocal(item.id); true }
            })
        }
        mapView.invalidate()
    }

    // Fit once per explicit camera request, after native layout. Neither weather updates,
    // recompositions nor later card-size changes reset a camera the user has moved.
    LaunchedEffect(mapView, selectedId, frameRequest, points) {
        // AndroidView may first receive an intermediate size before the information card
        // is measured. Wait for two layout frames with the same positive viewport.
        var measured: IntSize
        do {
            measured = snapshotFlow { mapSize }.first { it.width > 0 && it.height > 0 }
            withFrameNanos { }
            withFrameNanos { }
        } while (measured != mapSize || !mapView.isLaidOut ||
            mapView.width != measured.width || mapView.height != measured.height)
        mapView.controller.stopAnimation(false)
        if (point == null) mapView.fitPickupPoints(points)
        else {
            mapView.controller.setZoom(16.0)
            mapView.controller.setCenter(GeoPoint(point.latitude, point.longitude))
        }
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

    Column(Modifier.fillMaxSize().imePadding().clipToBounds().testTag("map-screen")) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(start = 10.dp, end = 10.dp, bottom = 4.dp)) {
            ReferenceTitle("Puntos de retiro", onBack)
            Text("Selecciona un local", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                SegmentedButton(selected = selectedId == null, onClick = { selectLocal(null) },
                    shape = SegmentedButtonDefaults.itemShape(0, points.size + 1)) {
                    Text("Ver todos", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                points.forEachIndexed { index, item ->
                    SegmentedButton(selected = item.id == selectedId,
                        onClick = { selectLocal(item.id) },
                        shape = SegmentedButtonDefaults.itemShape(index + 1, points.size + 1)) {
                        Text(item.title.removePrefix("Punto ").removeSuffix(" (demo)"), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            OutlinedTextField(query, { input ->
                query = input
                if (input.isBlank()) { selectedId = null; frameRequest++ }
                else points.firstOrNull { it.title.contains(input, true) || it.reference.contains(input, true) }
                    ?.let { selectedId = it.id; frameRequest++ }
            }, label = { Text("Buscar puntos de retiro") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (visiblePoints.isEmpty()) Text("Sin puntos coincidentes", style = MaterialTheme.typography.bodySmall)
        }
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()
            .onSizeChanged { mapSize = it })
        if (!keyboardVisible) Surface(modifier = Modifier.fillMaxWidth().padding(10.dp),
            shape = MaterialTheme.shapes.medium, shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (point == null) {
                    Text("Vista general · ${points.size} locales", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                    points.forEach { item ->
                        Text("${item.title.removeSuffix(" (demo)")} · ${item.reference}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Selecciona un local para ver su horario, indicaciones y el tiempo actual.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(point.title.removeSuffix(" (demo)"), style = MaterialTheme.typography.titleSmall)
                    Text(point.reference, style = MaterialTheme.typography.bodySmall)
                    Text(point.schedule.replace("Horario ficticio:", "Horario referencial:"),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    PickupWeatherCard(if (weatherState.pointId == point.id) weatherState
                        else PickupWeatherState(point.id, loading = true), onRetryWeather)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { point?.let { openExternal(DemoPickupPoints.directionsUrl(it)) } },
                        enabled = point != null, modifier = Modifier.weight(1f)) { Text("Cómo llegar") }
                    OutlinedButton(onClick = { frameRequest++ }, modifier = Modifier.weight(1f)) { Text("Centrar mapa") }
                }
                TextButton(onClick = { point?.let { openExternal(DemoPickupPoints.browserMapUrl(it)) } },
                    enabled = point != null) { Text("Abrir mapa en el navegador") }
                externalError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text("Ubicaciones y horarios de ejemplo; no corresponden a sucursales reales. Clima: Open-Meteo. © OpenStreetMap contributors.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun MapView.fitPickupPoints(points: List<PickupPoint>) {
    val locations = points.map { GeoPoint(it.latitude, it.longitude) }
    val margin = (32 * resources.displayMetrics.density).toInt().coerceAtMost(minOf(width, height) / 4)
    zoomToBoundingBox(BoundingBox.fromGeoPoints(locations), false, margin)
}
