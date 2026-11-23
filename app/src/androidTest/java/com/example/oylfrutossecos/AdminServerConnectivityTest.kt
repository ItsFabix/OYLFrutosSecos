package com.example.oylfrutossecos

import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.remote.OylRestService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Sonda opcional solo GET: no lee claves ni modifica datos del servidor de la prueba manual. */
class AdminServerConnectivityTest {
    @Test fun emulatorReachesPublicApiAndProtectedReadsRejectMissingCredentials() = runBlocking {
        val url = InstrumentationRegistry.getArguments().getString("oylReadOnlyProbeUrl")
        assumeTrue("Requiere oylReadOnlyProbeUrl", !url.isNullOrBlank())
        val api = Retrofit.Builder().baseUrl(url!!).addConverterFactory(GsonConverterFactory.create())
            .build().create(OylRestService::class.java)
        assertEquals("ok", api.health().status)
        assertTrue(api.products().isNotEmpty())
        try { api.adminOrders(""); fail("Debe exigir autorización") }
        catch (expected: HttpException) { assertEquals(403, expected.code()) }
        try { api.inventoryMovements(""); fail("Debe exigir autorización") }
        catch (expected: HttpException) { assertEquals(403, expected.code()) }
    }
}
