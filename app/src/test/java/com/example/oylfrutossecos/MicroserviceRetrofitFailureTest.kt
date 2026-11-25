package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.remote.OylRestService
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MicroserviceRetrofitFailureTest {
    @Test fun unavailablePickupServiceAndMissingOrderRemainHttpFailuresWithoutFabricatedResults() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(OylRestService::class.java)
            server.enqueue(MockResponse().setResponseCode(503).setBody("{\"message\":\"Servicio no disponible\"}"))
            try { api.pickupPoints(); fail("Unavailable pickup service must not become a successful empty catalog") }
            catch (error: HttpException) { assertEquals(503, error.code()) }
            val pickup = server.takeRequest()
            assertEquals("GET", pickup.method); assertEquals("/api/pickup-points", pickup.path)
            assertNull(pickup.getHeader("X-OYL-SERVICE-TOKEN"))
            assertNull(pickup.getHeader("X-OYL-ADMIN-KEY"))
            server.enqueue(MockResponse().setResponseCode(404))
            try { api.orderEvents("missing-order"); fail("Missing order must remain an error") }
            catch (error: HttpException) { assertEquals(404, error.code()) }
            val events = server.takeRequest()
            assertEquals("GET", events.method); assertEquals("/api/orders/missing-order/notifications", events.path)
            assertNull(events.getHeader("X-OYL-SERVICE-TOKEN"))
            server.enqueue(MockResponse().setBody("[]"))
            assertTrue(api.orderEvents("existing-order").isEmpty())
            assertEquals("/api/orders/existing-order/notifications", server.takeRequest().path)
        } finally { server.shutdown() }
    }
}
