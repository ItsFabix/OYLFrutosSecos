package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.remote.*
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AdminAuthorizedServiceTest {
    @Test fun administrative401And403InvalidateWithoutRetryAndKeepAuthorizationHeader() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val key = "contract-test-administrative-key"
            val rejected = mutableListOf<String>()
            val api = AdminAuthorizedService(Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(OylRestService::class.java)) { rejected += it }
            val product = ApiProductInput("Semillas", "Semillas", "Descripción", 2000, 250, 3, "seed")
            val calls: List<suspend () -> Any> = listOf(
                { api.adminOrders(key) }, { api.inventoryMovements(key) }, { api.advanceOrder("order-1", key) },
                { api.createProduct(product, key) }, { api.editProduct(1, product, key) },
                { api.adjustStock(1, ApiStockChange(1, "Reposición"), key) })
            calls.forEachIndexed { index, call ->
                val status = if (index % 2 == 0) 401 else 403
                server.enqueue(MockResponse().setResponseCode(status).setBody("private server details"))
                try { call(); fail("Protected request succeeded") }
                catch (expected: HttpException) { assertEquals(status, expected.code()) }
                assertEquals(key, server.takeRequest().getHeader("X-OYL-ADMIN-KEY"))
            }
            assertEquals(List(6) { key }, rejected)
            assertEquals(6, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun publicRequestRejectionDoesNotInvalidateAdministrativeSession() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            var rejected = false
            val api = AdminAuthorizedService(Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(OylRestService::class.java)) { rejected = true }
            server.enqueue(MockResponse().setResponseCode(403))
            try { api.products(); fail("Expected error") } catch (_: HttpException) { }
            assertFalse(rejected)
            assertNull(server.takeRequest().getHeader("X-OYL-ADMIN-KEY"))
        } finally { server.shutdown() }
    }

    @Test fun networkAndServerErrorsDoNotInvalidateOrRetryAdministrativeOperations() = runBlocking {
        for (failure in listOf(IOException("offline"), HttpException(retrofit2.Response.error<Any>(500,
            okhttp3.ResponseBody.create(null, "private detail"))))) {
            var calls = 0
            var rejected = false
            val api = AdminAuthorizedService(object : StubOylRestService() {
                override suspend fun adminOrders(key: String): List<ApiOrderResponse> { calls++; throw failure }
            }) { rejected = true }
            try { api.adminOrders("test-key"); fail("Expected error") } catch (actual: Exception) { assertSame(failure, actual) }
            assertFalse(rejected)
            assertEquals(1, calls)
        }
    }
}
