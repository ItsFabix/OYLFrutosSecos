package com.example.oylfrutossecos.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.oylfrutossecos.data.repository.DemoPickupPoints

/**
 * Mapa de puntos de referencia de DEMOSTRACION.
 * La pagina completa de OpenStreetMap funciona en el navegador del emulador.
 * En esta version usamos esa misma pagina, no el visor export/embed que podia quedar en blanco.
 * No se usan ubicaciones reales de OYL ni se requieren claves de APIs.
 */
@Composable
fun PickupMapScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val points = DemoPickupPoints.all
    var selectedId by remember { mutableStateOf(points.first().id) }
    var externalError by remember { mutableStateOf<String?>(null) }
    var pageLoading by remember { mutableStateOf(true) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var reloadCount by remember { mutableIntStateOf(0) }

    val point = points.first { it.id == selectedId }
    val mapUrl = DemoPickupPoints.browserMapUrl(point)
    val pageKey = "$mapUrl|$reloadCount"

    fun openExternal(url: String) {
        externalError = null
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            externalError = "No se encontro una aplicacion para abrir el mapa."
        } catch (_: SecurityException) {
            externalError = "El dispositivo no permitio abrir el mapa externo."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Volver") }
        Text("Mapa y puntos de retiro", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Datos ficticios para demostrar la aplicacion. Estos NO son locales ni puntos de retiro reales de OYL.",
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
                    mapError = null
                    pageLoading = true
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
        Box(modifier = Modifier.fillMaxWidth().height(325.dp)) {
            AndroidView(
                factory = { androidContext ->
                    WebView(androidContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                pageLoading = true
                                mapError = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageLoading = false
                            }

                            override fun onReceivedError(
                                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    pageLoading = false
                                    mapError = "No se pudo cargar el mapa: ${error?.description ?: "error de red"}."
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    pageLoading = false
                                    mapError = "OpenStreetMap respondio con HTTP ${response?.statusCode ?: "error"}."
                                }
                            }
                        }
                        tag = pageKey
                        loadUrl(mapUrl)
                    }
                },
                update = { webView ->
                    if (webView.tag != pageKey) {
                        webView.tag = pageKey
                        mapError = null
                        pageLoading = true
                        webView.loadUrl(mapUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (pageLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        if (mapError != null) {
            Text(mapError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(
            onClick = {
                mapError = null
                pageLoading = true
                reloadCount++
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text("Recargar mapa")
        }
        Text(
            "El visor muestra OpenStreetMap. Si queda en blanco, usa el boton del navegador o envia una captura del mensaje de error.",
            style = MaterialTheme.typography.bodySmall
        )

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
            "En la version final, las ubicaciones, direcciones y horarios vendran de informacion confirmada o de la API REST.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
