package com.example.oylfrutossecos

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.*
import com.example.oylfrutossecos.data.repository.*
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.runBlocking
import okhttp3.RequestBody
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.HttpException
import retrofit2.converter.gson.GsonConverterFactory

/** Opt-in: exige una URL y clave de un backend EXCLUSIVO de pruebas, nunca la BD del usuario. */
class RestRepositoryIntegrationTest {
    @Test fun realRetrofitBootSqliteLostResponseRecoveryAdminAndClient() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val url = args.getString("oylIntegrationBaseUrl")
        val key = args.getString("oylIntegrationAdminKey")
        assumeTrue("Requiere backend aislado y argumentos oylIntegrationBaseUrl/oylIntegrationAdminKey",
            !url.isNullOrBlank() && !key.isNullOrBlank())
        val api = Retrofit.Builder().baseUrl(url!!).addConverterFactory(GsonConverterFactory.create())
            .build().create(OylRestService::class.java)
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(base.cacheDir, "rest-integration/${UUID.randomUUID()}/oyl_auth.db")
        check(file.parentFile!!.mkdirs())
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String) = file
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?) =
                SQLiteDatabase.openOrCreateDatabase(file, factory)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?, handler: DatabaseErrorHandler?) =
                SQLiteDatabase.openDatabase(file.absolutePath, factory, SQLiteDatabase.CREATE_IF_NECESSARY, handler)
        }
        fun open() = OylDatabase::class.java.getDeclaredConstructor(Context::class.java)
            .apply { isAccessible = true }.newInstance(context)
        var database = open()
        try {
            val form = Registration("Cliente REST", "rest${UUID.randomUUID().toString().take(8)}", "rest@example.cl",
                "912345678", "Calle de prueba 123", "Cliente123", "Cliente123")
            val id = database.registerClient(form)
            database.saveSession(id)
            var user = AuthRepository(database).updateOwnProfile(id,
                ProfileDraft("Cliente REST Editado", "987654321", "Dirección editada 456"))
            val admin = database.authenticate("admin", "Admin1234", UserRole.ADMIN)!!
            var products = ProductRepository(database, api) { key!! }
            assertTrue(products.getProducts().size >= 8)
            val productId = products.saveProduct(null, ProductDraft("Semillas REST de prueba", "Semillas",
                "Bolsa de demostración aislada", 3000, 250, 3, "photo:private-only.jpg"))
            val product = database.readProducts().single { it.id == productId }
            assertNotNull(product.remoteId)
            assertEquals("photo:private-only.jpg", product.imageKey)
            assertEquals("seed", api.products().single { it.id == product.remoteId }.imageKey)
            database.setCartQuantity(user.id, productId, 2)
            val lostResponse = object : OylRestService by api {
                override suspend fun createPersistedOrder(request: RequestBody): ApiOrderResponse {
                    api.createPersistedOrder(request) // El servidor sí hizo commit.
                    throw IOException("Respuesta perdida después del commit real")
                }
            }
            try {
                OrderRepository(database, lostResponse).placeOrder(user, DeliveryMethod.DELIVERY, database.readCart(user.id))
                fail("No puede mostrar éxito si se perdió la respuesta")
            } catch (_: IllegalStateException) { }
            val pending = database.readPendingCheckout(user.id)!!
            assertTrue(database.readOrders(user.id).isEmpty())
            assertEquals(2, database.readCart(user.id).single().quantity)
            assertEquals(1, api.products().single { it.id == product.remoteId }.stock)
            // Editing a profile must not rewrite an already submitted checkout request.
            user = AuthRepository(database).updateOwnProfile(id,
                ProfileDraft("Cliente REST Actual", "976543210", "Otra dirección 789"))
            database.close()
            database = open()
            products = ProductRepository(database, api) { key!! }
            assertEquals(pending, database.readPendingCheckout(user.id))
            val orders = OrderRepository(database, api) { key!! }
            val placed = orders.placeOrder(user, DeliveryMethod.PICKUP, emptyList())
            assertTrue(database.readCart(user.id).isEmpty())
            assertNull(database.readPendingCheckout(user.id))
            assertEquals(1, database.readOrders(user.id).size)
            assertEquals(1, api.adminOrders(key!!).count { it.requestId == pending.requestId })
            assertEquals(1, api.products().single { it.id == product.remoteId }.stock)
            assertEquals("DELIVERY", api.getOrder(placed.serverId).delivery)
            assertEquals("987654321", api.getOrder(placed.serverId).contactPhone)
            assertEquals("Dirección editada 456", api.getOrder(placed.serverId).contactAddress)
            try { api.adminOrders(""); fail("La clave es obligatoria") }
            catch (expected: HttpException) { assertEquals(403, expected.code()) }
            for (status in listOf(OrderStatus.CONFIRMED, OrderStatus.PREPARED, OrderStatus.DELIVERED)) {
                val managed = orders.getOrders(admin).orders.single { it.remoteOrderId == placed.serverId }
                orders.advanceStatus(admin, managed)
                assertEquals(status, orders.getOrders(user).orders.single().status)
            }
            val inventory = InventoryRepository(database, api) { key!! }
            inventory.adjust(admin.id, productId, MovementType.ENTRY, 2, "Reposición de integración")
            assertEquals(3, api.products().single { it.id == product.remoteId }.stock)
            assertTrue(inventory.movements(productId).any { it.type == MovementType.EXIT && it.quantity == 2 })
            products.saveProduct(productId, ProductDraft("Semillas editadas REST", "Semillas", "Descripción editada", 3100, 250, 999))
            assertEquals(3, api.products().single { it.id == product.remoteId }.stock)
            database.close()
            database = open()
            assertEquals(OrderStatus.DELIVERED, database.readOrders(user.id).single().status)
            assertEquals("photo:private-only.jpg", database.readProducts().single { it.id == productId }.imageKey)
            val offline = object : StubOylRestService() { override suspend fun getOrder(id: String): ApiOrderResponse = throw IOException("offline") }
            val history = OrderRepository(database, offline).getOrders(user)
            assertFalse(history.remoteFresh)
            assertNotNull(history.error)
            assertEquals(OrderStatus.DELIVERED, history.orders.single().status)
        } finally { database.close() }
    }
}
