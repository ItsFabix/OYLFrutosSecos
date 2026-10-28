package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.local.*
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.*
import com.example.oylfrutossecos.data.repository.CheckoutCoordinator
import com.example.oylfrutossecos.domain.CheckoutLines
import com.example.oylfrutossecos.ui.viewmodel.CartState
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class CheckoutRecoveryTest {
    private val user = User(7, "Cliente", "cliente", "demo@example.cl", "911111111", "Santiago", UserRole.CLIENT)
    private val product = Product(1, "Almendras", "Frutos secos", "Bolsa", 3490, 250, 1, "nut")

    private inner class MemoryStore : CheckoutStore {
        var cart = listOf(CartItem(product, 1))
        var attempt: PendingCheckout? = null
        var completed = 0
        override fun readCart(userId: Long) = cart
        override fun pending(userId: Long) = attempt
        override fun getOrCreate(userId: Long, id: String, json: String,
                                 delivery: DeliveryMethod, cart: List<CartItem>): PendingCheckout {
            return attempt ?: PendingCheckout(id, json, delivery, CheckoutLines.fingerprint(cart))
                .also { attempt = it }
        }
        override fun clearRejected(userId: Long, requestId: String) {
            if (attempt?.requestId == requestId) attempt = null
        }
        override fun complete(user: User, remote: ApiOrderResponse): Long {
            check(attempt?.requestId == remote.requestId)
            completed++
            cart = emptyList()
            attempt = null
            return 1
        }
        override fun refreshProductCache(products: List<ApiProduct>) = Unit
    }

    private inner class Server(private val store: MemoryStore) : OylRestService {
        override suspend fun adminOrders(key: String): List<ApiOrderResponse> = error("Unused")
        override suspend fun advanceOrder(id: String, key: String): ApiOrderResponse = error("Unused")
        val bodies = mutableListOf<String>()
        val orders = mutableMapOf<String, ApiOrderResponse>()
        var stock = 1
        var loseNextResponse = false
        var error: HttpException? = null
        override suspend fun createPersistedOrder(request: RequestBody): ApiOrderResponse {
            val json = Buffer().also { request.writeTo(it) }.readUtf8()
            bodies += json
            assertEquals(store.attempt?.json, json) // Durable BEFORE sending, every time.
            error?.let { throw it }
            val saved = Gson().fromJson(json, ApiOrderRequest::class.java)
            val response = orders.getOrPut(saved.requestId) {
                check(stock > 0)
                stock--
                ApiOrderResponse("remote-1", saved.requestId, saved.customerAlias, "PENDING", 3490,
                    listOf(ApiOrderItem(1, "Almendras", 1, 3490)))
            }
            if (loseNextResponse) {
                loseNextResponse = false
                throw IOException("Response lost AFTER commit")
            }
            return response
        }
        override suspend fun health(): ApiHealth = error("Unused")
        override suspend fun products(): List<ApiProduct> = emptyList()
        override suspend fun createOrder(request: ApiOrderRequest): ApiOrderResponse =
            error("Checkout must send the persisted JSON directly")
        override suspend fun getOrder(id: String): ApiOrderResponse = error("Unused")
    }

    @Test fun normalPurchaseCreatesOneOrderAndOneStockDiscount() = runBlocking {
        val store = MemoryStore()
        val server = Server(store)
        CheckoutCoordinator(store, server).placeOrder(user, DeliveryMethod.PICKUP, store.cart)
        assertEquals(1, server.orders.size)
        assertEquals(0, server.stock)
        assertEquals(1, store.completed)
        assertNull(store.attempt)
        assertTrue(store.cart.isEmpty())
    }

    @Test fun lostResponseRecoversAfterCoordinatorRecreationWithZeroStock() = runBlocking {
        val store = MemoryStore()
        val server = Server(store).apply { loseNextResponse = true }
        try {
            CheckoutCoordinator(store, server).placeOrder(user, DeliveryMethod.DELIVERY, store.cart)
            fail("Expected a lost response")
        } catch (_: IllegalStateException) { }
        val persisted = store.attempt!!
        store.cart = store.cart.map { it.copy(product = it.product.copy(stock = 0, priceClp = 9000)) }
        val state = CartState(items = store.cart, loading = false, pending = persisted)
        assertTrue(state.canCheckout)
        assertFalse(state.canEdit)
        // New coordinator has no in-memory UUID. Current UI delivery and even an empty list
        // must not replace the durable payload of the pending attempt.
        CheckoutCoordinator(store, server).placeOrder(user, DeliveryMethod.PICKUP, emptyList())
        assertEquals(listOf(persisted.json, persisted.json), server.bodies)
        assertEquals(1, server.orders.size)
        assertEquals(0, server.stock)
        assertTrue(CartState(loading = false).canEdit)
    }

    @Test fun legacyAttemptKeepsExactJsonWhitespaceAndUnknownFields() = runBlocking {
        val store = MemoryStore()
        val json = """{ "items":[{"quantity":1,"productId":1}], "requestId":"old-request", "customerAlias":"cliente", "legacyExtra":true }"""
        store.attempt = PendingCheckout("old-request", json, DeliveryMethod.PICKUP, "1:1:3490")
        val server = Server(store)
        CheckoutCoordinator(store, server).placeOrder(user, DeliveryMethod.DELIVERY, emptyList())
        assertEquals(json, server.bodies.single())
    }

    @Test fun ambiguous409NeverDiscardsThePendingRequest() = runBlocking {
        assertRetainedAfterError(409, null)
    }

    @Test fun markerForAnotherRequestDoesNotAuthorizeDiscardingThisOne() = runBlocking {
        assertRetainedAfterError(409, "another-request")
    }

    @Test fun otherHttpErrorsAlsoPreserveTheRequest() = runBlocking {
        for (code in listOf(400, 404, 500)) assertRetainedAfterError(code, null)
    }

    private suspend fun assertRetainedAfterError(code: Int, marker: String?) {
        val store = MemoryStore()
        val saved = PendingCheckout("persisted-id", """{"requestId":"persisted-id"}""",
            DeliveryMethod.PICKUP, "1:1:3490")
        store.attempt = saved
        val server = Server(store).apply { error = httpError(code, marker) }
        try {
            CheckoutCoordinator(store, server).placeOrder(user, DeliveryMethod.PICKUP, store.cart)
            fail("Expected HTTP failure")
        } catch (_: IllegalStateException) { }
        assertEquals(saved, store.attempt)
        assertTrue(CartState(store.cart, loading = false, pending = store.attempt).canCheckout)
    }

    @Test fun definitiveStockRejectionUnlocksCartWithoutChangingItsItems() = runBlocking {
        val store = MemoryStore()
        store.attempt = PendingCheckout("persisted-id", "{}", DeliveryMethod.PICKUP, "1:1:3490")
        val original = store.cart
        val server = Server(store).apply { error = httpError(409, "persisted-id") }
        try {
            CheckoutCoordinator(store, server).placeOrder(user, DeliveryMethod.PICKUP, store.cart)
            fail("Expected rejection")
        } catch (_: IllegalStateException) { }
        assertNull(store.attempt)
        assertEquals(original, store.cart)
        assertTrue(CartState(store.cart, loading = false).canEdit)
        assertEquals(1, server.stock)
    }

    @Test fun stableLineIdentityIgnoresPriceAndSupportsLegacyFingerprints() {
        val before = listOf(CartItem(product, 1))
        val after = listOf(CartItem(product.copy(priceClp = 8000), 1))
        assertEquals(CheckoutLines.fingerprint(before), CheckoutLines.fingerprint(after))
        assertEquals(CheckoutLines.fingerprint(after), CheckoutLines.normalizeStored("1:1:3490"))
        assertEquals("1:1;2:3", CheckoutLines.normalizeStored("2:3:7000;1:1:3490"))
        assertNull(CheckoutLines.normalizeStored("1:1:3490;1:2:3490"))
    }

    @Test fun normalCartWithInsufficientStockCannotCheckoutButPendingOneCan() {
        val items = listOf(CartItem(product.copy(stock = 0), 1))
        assertFalse(CartState(items, loading = false).canCheckout)
        val attempt = PendingCheckout("saved", "{}", DeliveryMethod.PICKUP, "1:1")
        assertTrue(CartState(items, loading = false, pending = attempt).canCheckout)
        assertTrue(CartState(loading = false, pending = attempt).canCheckout)
        assertFalse(CartState(items, loading = true, pending = attempt).canCheckout)
    }

    private fun httpError(code: Int, marker: String?): HttpException {
        val raw = okhttp3.Response.Builder().request(okhttp3.Request.Builder().url("http://localhost/").build())
            .protocol(okhttp3.Protocol.HTTP_1_1).code(code).message("Rejected")
            .apply { if (marker != null) header(CheckoutCoordinator.REJECTED_REQUEST_HEADER, marker) }.build()
        return HttpException(Response.error<ApiOrderResponse>(
            ResponseBody.create(MediaType.parse("application/json"), "{}"), raw))
    }
}
