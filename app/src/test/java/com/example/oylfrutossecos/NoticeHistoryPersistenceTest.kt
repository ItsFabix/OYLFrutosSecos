package com.example.oylfrutossecos

import android.content.ContentValues
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.*
import com.example.oylfrutossecos.ui.components.noticeTitle
import com.example.oylfrutossecos.ui.components.noticeMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28], manifest=Config.NONE)
class NoticeHistoryPersistenceTest : RepositorySqliteSupport() {
    @Test fun oldSqliteNoticesArePresentedProfessionallyWithoutWritingOrLosingOwnership() {
        val owner = user(); val other = user("otro")
        db.writableDatabase.insertOrThrow("app_notices", null, ContentValues().apply {
            put("user_id", owner.id); put("kind", "ORDER_STATUS")
            put("title", "Pedido REST #5: Entregado"); put("message", "El servidor actualizó el estado de tu pedido.")
            put("target", "orders"); put("created_at", 1234L); put("read_at", 1500L)
        })
        val before = db.readNotices(owner.id)
        val saved = before.single()
        repeat(3) {
            assertEquals("Pedido #5 entregado", noticeTitle(saved))
            assertEquals("Tu pedido ha sido entregado. ¡Gracias por tu compra!", noticeMessage(saved))
        }
        assertEquals(before, db.readNotices(owner.id)); assertTrue(db.readNotices(other.id).isEmpty())
        reopenSandbox()
        assertEquals(before, db.readNotices(owner.id))
    }

    @Test fun newReceiptsAndStatusUpdatesAreAccurateAndRefreshingNeverDuplicatesNotices() = runBlocking {
        for (method in DeliveryMethod.entries) {
            val owner = user(method.name.lowercase())
            val product = ProductRepository(db, api).getProducts().single()
            db.setCartQuantity(owner.id, product.id, 1)
            val repo = OrderRepository(db, api)
            val placed = repo.placeOrder(owner, method, db.readCart(owner.id))
            assertEquals("Pedido #${placed.localId} recibido", noticeTitle(db.readNotices(owner.id).single()))
            assertTrue(noticeMessage(db.readNotices(owner.id).single()).contains("pendiente de confirmación"))
            val remote = api.orders.values.single { it.id == placed.serverId }
            api.orders[remote.requestId] = remote.copy(status = "PREPARED")
            repo.getOrders(owner)
            val updated = db.readNotices(owner.id)
            assertEquals(2, updated.size)
            assertTrue(noticeMessage(updated.first()).contains(if (method == DeliveryMethod.PICKUP) "listo para retirar" else "preparado para despacho"))
            repo.getOrders(owner); repo.getOrders(owner)
            assertEquals(updated, db.readNotices(owner.id))
            reopenSandbox()
            assertEquals(updated, db.readNotices(owner.id))
        }
    }
}
