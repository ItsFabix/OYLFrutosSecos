package com.example.oylfrutossecos

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.local.*
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.*
import com.example.oylfrutossecos.data.repository.CheckoutCoordinator
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.runBlocking
import okhttp3.RequestBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real SQLite, in a unique cache directory. Never uses the singleton or the user's oyl_auth.db. */
@RunWith(AndroidJUnit4::class)
class CheckoutSqliteRecoveryTest {
    private lateinit var context: Context
    private lateinit var database: OylDatabase
    private lateinit var client: User
    private lateinit var otherClient: User

    @Before fun setUp() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(base.cacheDir, "checkout-tests/${UUID.randomUUID()}/oyl_auth.db")
        check(file.parentFile!!.mkdirs())
        context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String): File = file
            override fun openOrCreateDatabase(name: String, mode: Int,
                factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
                SQLiteDatabase.openOrCreateDatabase(file, factory)
            override fun openOrCreateDatabase(name: String, mode: Int,
                factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
                SQLiteDatabase.openDatabase(file.absolutePath, factory,
                    SQLiteDatabase.CREATE_IF_NECESSARY, errorHandler)
        }
        database = openIsolatedDatabase()
        fun register(alias: String): User {
            val id = database.registerClient(Registration("Cliente", alias, "$alias@example.cl",
                "911111111", "Santiago", "Cliente123", "Cliente123"))
            return User(id, "Cliente", alias, "$alias@example.cl", "911111111", "Santiago", UserRole.CLIENT)
        }
        client = register("recovery")
        otherClient = register("other")
        database.setCartQuantity(client.id, 1, 1)
        database.setCartQuantity(otherClient.id, 1, 2)
    }

    private fun openIsolatedDatabase(): OylDatabase =
        OylDatabase::class.java.getDeclaredConstructor(Context::class.java)
            .apply { isAccessible = true }.newInstance(context)

    @After fun closeOnlyTheTestDatabase() { database.close() }

    private fun createPending(legacy: Boolean = false): PendingCheckout {
        val store = RemoteCheckoutCache(database)
        val request = ApiOrderRequest(UUID.randomUUID().toString(), client.alias,
            listOf(ApiOrderItemRequest(1, 1)), "DELIVERY", client.phone, client.address)
        val pending = store.getOrCreate(client.id, request.requestId, Gson().toJson(request),
            DeliveryMethod.DELIVERY, database.readCart(client.id))
        if (legacy) database.writableDatabase.update("pending_remote_checkout",
            ContentValues().apply { put("cart_fingerprint", "1:1:3490") },
            "user_id = ?", arrayOf(client.id.toString()))
        return store.pending(client.id) ?: pending
    }

    private fun response(pending: PendingCheckout) = ApiOrderResponse(UUID.randomUUID().toString(),
        pending.requestId, client.alias, "PENDING", 3490, listOf(ApiOrderItem(1, "Almendras", 1, 3490)))

    @Test fun legacyPendingSurvivesCloseAndReopenAndPriceChange() {
        val saved = createPending(legacy = true)
        database.close()
        database = openIsolatedDatabase()
        assertEquals(saved, RemoteCheckoutCache(database).pending(client.id))
        database.writableDatabase.execSQL("UPDATE products SET price_clp = 9000, stock = 0 WHERE id = 1")
        RemoteCheckoutCache(database).complete(client, response(saved))
        assertTrue(database.readCart(client.id).isEmpty())
        assertEquals(2, database.readCart(otherClient.id).single().quantity)
        assertEquals(1, database.readOrders(client.id).size)
        assertNull(RemoteCheckoutCache(database).pending(client.id))
    }

    @Test fun currentPendingPriceChangeRemovesOnlyPurchasedLines() {
        val saved = createPending()
        // Simulate an unexpected extra line; completion must preserve it and other users' carts.
        database.writableDatabase.execSQL("INSERT INTO cart_items VALUES (?, 2, 1)", arrayOf(client.id))
        database.writableDatabase.execSQL("UPDATE products SET price_clp = 9000 WHERE id = 1")
        RemoteCheckoutCache(database).complete(client, response(saved))
        assertEquals(2L, database.readCart(client.id).single().product.id)
        assertEquals(2, database.readCart(otherClient.id).single().quantity)
    }

    @Test fun pendingCartCannotBeEditedEvenAfterReopeningDatabase() {
        createPending()
        database.close()
        database = openIsolatedDatabase()
        for (quantity in listOf(0, 2)) {
            try {
                database.setCartQuantity(client.id, 1, quantity)
                fail("Pending cart must remain frozen")
            } catch (_: IllegalStateException) { }
        }
        assertEquals(1, database.readCart(client.id).single().quantity)
        // Another user can still edit their own cart.
        database.setCartQuantity(otherClient.id, 1, 3)
        assertEquals(3, database.readCart(otherClient.id).single().quantity)
    }

    @Test fun invalidResponseRollsBackHistoryCartAndPendingChanges() {
        val saved = createPending()
        try {
            RemoteCheckoutCache(database).complete(client, response(saved).copy(
                items = listOf(ApiOrderItem(2, "Different product", 1, 3490))))
            fail("Mismatched lines must not complete a checkout")
        } catch (_: IllegalStateException) { }
        assertEquals(saved, RemoteCheckoutCache(database).pending(client.id))
        assertEquals(1, database.readCart(client.id).single().quantity)
        assertTrue(database.readOrders(client.id).isEmpty())
    }

    @Test fun lostResponseThenReopenedSqliteRecoversWithoutSecondServerDiscount() = runBlocking {
        val bodies = mutableListOf<String>()
        var committed: ApiOrderResponse? = null
        var discounts = 0
        var loseResponse = true
        val api = object : OylRestService {
            override suspend fun createPersistedOrder(request: RequestBody): ApiOrderResponse {
                val json = Buffer().also { request.writeTo(it) }.readUtf8()
                bodies += json
                val saved = RemoteCheckoutCache(database).pending(client.id)!!
                assertEquals(saved.json, json)
                if (committed == null) { committed = response(saved); discounts++ }
                if (loseResponse) { loseResponse = false; throw IOException("Lost after commit") }
                return committed!!
            }
            override suspend fun products(): List<ApiProduct> = emptyList()
            override suspend fun health(): ApiHealth = error("Unused")
            override suspend fun createOrder(request: ApiOrderRequest): ApiOrderResponse = error("Unused")
            override suspend fun getOrder(id: String): ApiOrderResponse = error("Unused")
        }
        try {
            CheckoutCoordinator(RemoteCheckoutCache(database), api).placeOrder(client,
                DeliveryMethod.DELIVERY, database.readCart(client.id))
            fail("Expected response loss")
        } catch (_: IllegalStateException) { }
        database.close()
        database = openIsolatedDatabase()
        database.writableDatabase.execSQL("UPDATE products SET stock = 0, price_clp = 9000 WHERE id = 1")
        CheckoutCoordinator(RemoteCheckoutCache(database), api).placeOrder(client,
            DeliveryMethod.PICKUP, emptyList())
        assertEquals(bodies[0], bodies[1])
        assertEquals(1, discounts)
        assertEquals(1, database.readOrders(client.id).size)
        assertTrue(database.readCart(client.id).isEmpty())
        assertNull(RemoteCheckoutCache(database).pending(client.id))
    }
}
