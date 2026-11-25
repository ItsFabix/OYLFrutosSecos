package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.remote.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitContractTest {
    @Test fun productsAndAdministrativeCallsUseExpectedRoutesHeadersAndBodies() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(OylRestService::class.java)
            val product = """{"id":1000,"name":"Semillas","category":"Semillas","description":"Bolsa","priceClp":3000,"weightGrams":250,"stock":5,"imageKey":"seed"}"""
            server.enqueue(MockResponse().setBody("[$product]").setHeader("Content-Type", "application/json"))
            assertEquals(1000L, api.products().single().id)
            assertEquals("/api/products", server.takeRequest().path)
            server.enqueue(MockResponse().setBody(product))
            api.editProduct(1000, ApiProductInput("Semillas", "Semillas", "Bolsa", 3000, 250, 999), "session-key")
            val edit = server.takeRequest()
            assertEquals("PUT", edit.method)
            assertEquals("/api/admin/products/1000", edit.path)
            assertEquals("session-key", edit.getHeader("X-OYL-ADMIN-KEY"))
            assertTrue(edit.body.readUtf8().contains("\"stock\":999"))
            server.enqueue(MockResponse().setBody("[]"))
            api.adminOrders("session-key")
            val admin = server.takeRequest()
            assertEquals("GET", admin.method)
            assertEquals("/api/admin/orders", admin.path)
            assertEquals("session-key", admin.getHeader("X-OYL-ADMIN-KEY"))
            server.enqueue(MockResponse().setBody("[]"))
            api.inventoryMovements("session-key")
            assertEquals("/api/admin/inventory/movements", server.takeRequest().path)
            server.enqueue(MockResponse().setBody("""{"productId":1000,"oldStock":5,"newStock":6}"""))
            assertEquals(6, api.adjustStock(1000, ApiStockChange(1, "Entrada prueba"), "session-key").newStock)
            assertEquals("/api/admin/products/1000/stock", server.takeRequest().path)
        } finally { server.shutdown() }
    }

    @Test fun newMicroserviceRoutesUseExistingAndroidBaseUrl() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(OylRestService::class.java)
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
                .setBody("""[{"id":"centro","title":"Centro","reference":"Plaza",
                "schedule":"Lun a vie","latitude":-33.4372,"longitude":-70.6506}]"""))
            assertEquals("centro", api.pickupPoints().single().id)
            assertEquals("/api/pickup-points", server.takeRequest().path)

            val id = "123e4567-e89b-12d3-a456-426614174000"
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
                .setBody("""[{"eventId":"order:sample:created","orderId":"$id",
                "kind":"ORDER_CREATED","status":"PENDING","createdAt":1730000000000,
                "read":false}]"""))
            assertEquals("ORDER_CREATED", api.orderEvents(id).single().kind)
            assertEquals("/api/orders/$id/notifications", server.takeRequest().path)
        } finally { server.shutdown() }
    }
}
