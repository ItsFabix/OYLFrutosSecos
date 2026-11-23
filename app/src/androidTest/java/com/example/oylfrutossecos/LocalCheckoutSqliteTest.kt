package com.example.oylfrutossecos

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.local.RemoteCheckoutCache
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.*
import com.example.oylfrutossecos.ui.viewmodel.OrderViewModel
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** SQLite real en un archivo temporal propio; nunca abre la base ni las fotos del usuario. */
class LocalCheckoutSqliteTest {
    // Conserva la cobertura de SQLite anterior sin habilitar compras locales en producción.
    private class LegacyOrders(private val database: OylDatabase) {
        fun placeOrder(user: User, delivery: DeliveryMethod, items: List<CartItem>) = database.placeDemoOrder(user, delivery, items)
        fun getOrders(user: User) = database.readOrders(if (user.role == UserRole.ADMIN) null else user.id)
        fun advanceStatus(user: User, id: Long) {
            require(user.role == UserRole.ADMIN)
            database.advanceDemoOrder(id)
        }
    }
    private lateinit var database: OylDatabase
    private lateinit var context: Context
    private lateinit var client: User
    private lateinit var other: User
    private lateinit var admin: User
    private val store = ViewModelStore()

    private fun openDatabase(): OylDatabase =
        OylDatabase::class.java.getDeclaredConstructor(Context::class.java)
            .apply { isAccessible = true }.newInstance(context)

    @Before fun isolatedDatabase() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(base.cacheDir, "local-checkout-tests/${UUID.randomUUID()}/oyl_auth.db")
        check(file.parentFile!!.mkdirs())
        context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String): File = file
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?) =
                SQLiteDatabase.openOrCreateDatabase(file, factory)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?,
                errorHandler: DatabaseErrorHandler?) = SQLiteDatabase.openDatabase(file.absolutePath, factory,
                    SQLiteDatabase.CREATE_IF_NECESSARY, errorHandler)
        }
        database = openDatabase()
        fun register(alias: String): User {
            val form = Registration("Cliente ${alias}", alias, "${alias}@example.cl", "912345678",
                "Calle del Sur 123", "Cliente123", "Cliente123")
            val id = database.registerClient(form)
            return User(id, form.name, form.alias, form.email, form.phone, form.address, UserRole.CLIENT)
        }
        client = register("comprador")
        other = register("otrocliente")
        admin = database.authenticate("admin", "Admin1234", UserRole.ADMIN)!!
    }

    @After fun closeResources() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        database.close()
    }

    private fun product() = database.readProducts().first { it.stock >= 3 }

    private suspend fun rejects(block: suspend () -> Unit) {
        var rejected = false
        try { block() } catch (_: IllegalArgumentException) { rejected = true }
        catch (_: IllegalStateException) { rejected = true }
        assertTrue("La operación inválida debe rechazarse", rejected)
    }

    @Test fun localCheckoutSurvivesReopeningAndAdminChangesAreVisibleOnlyToOwner() = runBlocking {
        val selected = product()
        val auth = AuthRepository(database)
        assertEquals(client, auth.login(client.alias, "Cliente123", UserRole.CLIENT))
        CartRepository(database).setQuantity(client.id, selected.id, 2)
        CartRepository(database).setQuantity(other.id, selected.id, 1)
        database.close()
        database = openDatabase()
        assertEquals(client, AuthRepository(database).currentUser())
        assertEquals(2, CartRepository(database).getCart(client.id).single().quantity)
        val repository = LegacyOrders(database)
        val cart = CartRepository(database).getCart(client.id)
        val id = repository.placeOrder(client, DeliveryMethod.PICKUP, cart)
        assertTrue(CartRepository(database).getCart(client.id).isEmpty())
        assertEquals(1, CartRepository(database).getCart(other.id).single().quantity)
        assertEquals(selected.stock, database.readProducts().first { it.id == selected.id }.stock)
        assertTrue(repository.getOrders(other).isEmpty())
        assertNull(repository.getOrders(client).single().remoteOrderId)
        assertEquals(id, repository.getOrders(admin).single().id)
        assertEquals(OrderStatus.PENDING, repository.getOrders(client).single().status)
        rejects { repository.placeOrder(client, DeliveryMethod.PICKUP, cart) }
        assertEquals(1, repository.getOrders(admin).size)
        rejects { repository.advanceStatus(client, id) }
        for (status in listOf(OrderStatus.CONFIRMED, OrderStatus.PREPARED, OrderStatus.DELIVERED)) {
            repository.advanceStatus(admin, id)
            assertEquals(status, repository.getOrders(client).single().status)
        }
        rejects { repository.advanceStatus(admin, id) }
        assertEquals(3, database.readNotices(client.id).count { it.kind == NoticeKind.ORDER_STATUS })
        assertTrue(database.readNotices(other.id).none { it.kind == NoticeKind.ORDER_STATUS })
        database.close()
        database = openDatabase()
        assertEquals(OrderStatus.DELIVERED, LegacyOrders(database).getOrders(client).single().status)
        assertEquals(selected.stock, database.readProducts().first { it.id == selected.id }.stock)
    }

    @Test fun changedPriceAndInsufficientStockRollbackWithoutLosingCartOrCreatingNotices() = runBlocking {
        val selected = product()
        database.setCartQuantity(client.id, selected.id, 2)
        val old = database.readCart(client.id)
        val noticeCount = database.readNotices(client.id).size
        database.writableDatabase.execSQL("UPDATE products SET price_clp = price_clp + 100 WHERE id = ?",
            arrayOf(selected.id))
        rejects { LegacyOrders(database).placeOrder(client, DeliveryMethod.DELIVERY, old) }
        assertTrue(database.readOrders(null).isEmpty())
        assertEquals(noticeCount, database.readNotices(client.id).size)
        assertEquals(2, database.readCart(client.id).single().quantity)
        val updated = database.readCart(client.id)
        database.writableDatabase.execSQL("UPDATE products SET stock = 1 WHERE id = ?", arrayOf(selected.id))
        rejects { LegacyOrders(database).placeOrder(client, DeliveryMethod.DELIVERY, updated) }
        assertTrue(database.readOrders(null).isEmpty())
        assertEquals(2, database.readCart(client.id).single().quantity)
    }

    @Test fun legacyPendingRequestIsPreservedExactlyAndBlocksOnlyItsOwner() = runBlocking {
        val selected = product()
        database.setCartQuantity(client.id, selected.id, 1)
        database.setCartQuantity(other.id, selected.id, 1)
        val cart = database.readCart(client.id)
        val json = """{ "requestId":"old-id", "legacy":true, "items":[{"productId":${selected.id},"quantity":1}] }"""
        RemoteCheckoutCache(database).getOrCreate(client.id, "old-id", json, DeliveryMethod.PICKUP, cart)
        // Simula una huella de la implementación anterior, que también incluía el precio.
        database.writableDatabase.execSQL("UPDATE pending_remote_checkout SET cart_fingerprint = ? WHERE user_id = ?",
            arrayOf("${selected.id}:1:${selected.priceClp}", client.id))
        val original = database.readPendingCheckout(client.id)!!
        database.close()
        database = openDatabase()
        assertEquals(original, CartRepository(database).snapshot(client.id).pending)
        rejects { LegacyOrders(database).placeOrder(client, DeliveryMethod.DELIVERY, cart) }
        rejects { CartRepository(database).setQuantity(client.id, selected.id, 0) }
        assertEquals(original, database.readPendingCheckout(client.id))
        assertEquals(json, database.readPendingCheckout(client.id)!!.json)
        assertEquals(1, database.readCart(client.id).single().quantity)
        LegacyOrders(database).placeOrder(other, DeliveryMethod.PICKUP, database.readCart(other.id))
        assertEquals(original, database.readPendingCheckout(client.id))
        assertEquals(other.id, database.readOrders(null).single().userId)
    }

    @Test fun catalogAndAuditedInventoryStayLocalAfterRepositoryRecreation() = runBlocking {
        val products = database
        val id = products.saveProduct(null, ProductDraft("Nueces del Sur", "Frutos secos",
            "Nueces seleccionadas en bolsa.", 4200, 250, 8))
        database.recordInventoryMovement(admin.id, id, MovementType.EXIT, 2, "Salida controlada")
        database.close()
        database = openDatabase()
        val saved = database.readProducts().first { it.id == id }
        assertEquals("Nueces del Sur", saved.name)
        assertEquals(6, saved.stock)
        val movement = database.readInventoryMovements(id).single()
        assertEquals(8, movement.beforeStock)
        assertEquals(6, movement.afterStock)
        assertEquals("Salida controlada", movement.note)
    }

    @Test fun viewModelRejectsRapidDuplicateConfirmation() = runBlocking {
        val selected = product()
        database.setCartQuantity(client.id, selected.id, 1)
        val items = database.readCart(client.id)
        lateinit var vm: OrderViewModel
        var completions = 0
        val api = object : StubOylRestService() {
            override suspend fun products() = listOf(com.example.oylfrutossecos.data.remote.ApiProduct(
                selected.id, selected.name, selected.category, selected.description, selected.priceClp,
                selected.weightGrams, selected.stock, selected.imageKey))
            override suspend fun createPersistedOrder(request: okhttp3.RequestBody): com.example.oylfrutossecos.data.remote.ApiOrderResponse {
                delay(100)
                val body = okio.Buffer().also { request.writeTo(it) }.readUtf8()
                val saved = com.google.gson.Gson().fromJson(body, com.example.oylfrutossecos.data.remote.ApiOrderRequest::class.java)
                return com.example.oylfrutossecos.data.remote.ApiOrderResponse("vm-test-order", saved.requestId,
                    saved.customerAlias, "PENDING", selected.priceClp.toLong(),
                    listOf(com.example.oylfrutossecos.data.remote.ApiOrderItem(selected.id, selected.name, 1, selected.priceClp)))
            }
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            vm = ViewModelProvider(store, OrderViewModel.Factory(OrderRepository(database, api), client))[OrderViewModel::class.java]
            vm.place(DeliveryMethod.PICKUP, items) { completions++ }
            vm.place(DeliveryMethod.PICKUP, items) { completions++ }
        }
        withTimeout(30000) { while (vm.state.value.placedId == null || vm.state.value.busy) delay(20) }
        InstrumentationRegistry.getInstrumentation().runOnMainSync { assertEquals(1, completions) }
        assertEquals(1, database.readOrders(null).size)
        assertTrue(database.readCart(client.id).isEmpty())
    }

    @Test fun failureWritingOrderLinesRollsBackHeaderCartAndNotices() = runBlocking {
        val selected = product()
        database.setCartQuantity(client.id, selected.id, 1)
        val cart = database.readCart(client.id)
        val clientNotices = database.readNotices(client.id)
        val adminNotices = database.readNotices(admin.id)
        database.writableDatabase.execSQL("""
            CREATE TRIGGER fail_test_order_line BEFORE INSERT ON demo_order_lines
            BEGIN SELECT RAISE(ABORT, 'Simulated SQLite write failure'); END
        """.trimIndent())
        try {
            LegacyOrders(database).placeOrder(client, DeliveryMethod.PICKUP, cart)
            fail("La escritura debe fallar después de insertar la cabecera")
        } catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(database.readOrders(null).isEmpty())
        assertEquals(cart, database.readCart(client.id))
        assertEquals(clientNotices, database.readNotices(client.id))
        assertEquals(adminNotices, database.readNotices(admin.id))
    }

    @Test fun historicalRemoteOrderIsReadOnlyAndAdminCannotCreateClientOrder() = runBlocking {
        val selected = product()
        database.setCartQuantity(client.id, selected.id, 1)
        val repository = LegacyOrders(database)
        rejects { repository.placeOrder(admin, DeliveryMethod.PICKUP, database.readCart(client.id)) }
        val id = repository.placeOrder(client, DeliveryMethod.PICKUP, database.readCart(client.id))
        database.writableDatabase.execSQL("UPDATE demo_orders SET server_order_id = ? WHERE id = ?",
            arrayOf("historical-remote", id))
        rejects { repository.advanceStatus(admin, id) }
        assertEquals(OrderStatus.PENDING, repository.getOrders(client).single().status)
        assertEquals("historical-remote", repository.getOrders(client).single().remoteOrderId)
    }
}
