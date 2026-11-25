package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE)
class OrderRepositoryTest:RepositorySqliteSupport() {
    private suspend fun prepare(owner:User,quantity:Int=2):List<CartItem> {
        val product=ProductRepository(db,api).getProducts().single()
        db.setCartQuantity(owner.id,product.id,quantity)
        return db.readCart(owner.id)
    }
    @Test fun successfulCheckoutStoresRemoteIdentityClearsOnlyOwnerCartAndRefreshesStock()=runBlocking {
        val owner=user();val other=user("otro")
        val cart=prepare(owner);db.setCartQuantity(other.id,cart.single().product.id,1)
        val repo=OrderRepository(db,api)
        val placed=repo.placeOrder(owner,DeliveryMethod.DELIVERY,cart)
        assertEquals(placed.serverId,db.readOrders(owner.id).single().remoteOrderId)
        assertEquals(placed.localId,db.readOrders(owner.id).single().id)
        assertTrue(db.readCart(owner.id).isEmpty());assertEquals(1,db.readCart(other.id).single().quantity)
        assertEquals(3,db.readProducts().single {it.remoteId==1L}.stock)
        assertNull(db.readPendingCheckout(owner.id))
        val history=repo.getOrders(owner)
        assertTrue(history.remoteFresh);assertNull(history.error)
        assertEquals(owner.phone,history.orders.single().contactPhone)
        assertEquals(owner.address,history.orders.single().contactAddress)
    }
    @Test fun lostResponseRetryUsesExactStoredRequestAndNeverDuplicatesOrderOrStockDeduction()=runBlocking {
        val owner=user();val cart=prepare(owner);val repo=OrderRepository(db,api)
        api.loseOrderResponse=true
        try {repo.placeOrder(owner,DeliveryMethod.DELIVERY,cart);fail("must not confirm lost response")} catch(_:IllegalStateException) {}
        val pending=db.readPendingCheckout(owner.id)!!
        assertEquals(1,api.orders.size);assertEquals(3,api.catalog.single().stock)
        assertTrue(db.readOrders(owner.id).isEmpty());assertEquals(cart,db.readCart(owner.id))
        api.loseOrderResponse=false;api.productsFailure=java.io.IOException("catalog refresh offline")
        reopenSandbox()
        assertEquals(pending,db.readPendingCheckout(owner.id))
        val placed=OrderRepository(db,api).placeOrder(owner.copy(address="Dirección diferente"),DeliveryMethod.PICKUP,emptyList())
        assertEquals(pending.json,api.submittedJson.last());assertEquals(api.submittedJson.first(),api.submittedJson.last())
        assertEquals(1,api.orders.size);assertEquals(3,api.catalog.single().stock)
        assertEquals(placed.serverId,db.readOrders(owner.id).single().remoteOrderId)
        assertNull(db.readPendingCheckout(owner.id));assertTrue(db.readCart(owner.id).isEmpty())
        assertEquals(owner.address,db.readOrders(owner.id).single().contactAddress)
    }
    @Test fun historiesSeparateLegacyAndRemoteOrdersAndNeverQueryOtherUsersIds()=runBlocking {
        val owner=user();val other=user("otro")
        db.setCartQuantity(owner.id,1,1)
        val legacy=db.placeDemoOrder(owner,DeliveryMethod.PICKUP,db.readCart(owner.id))
        val repo=OrderRepository(db,api) {key}
        val first=repo.placeOrder(owner,DeliveryMethod.PICKUP,prepare(owner,1))
        val second=repo.placeOrder(other,DeliveryMethod.PICKUP,prepare(other,1))
        api.queriedOrders.clear()
        val client=repo.getOrders(owner)
        assertEquals(2,client.orders.size);assertTrue(client.orders.any {it.id==legacy&&it.remoteOrderId==null})
        assertEquals(listOf(first.serverId),api.queriedOrders)
        assertFalse(client.orders.any {it.remoteOrderId==second.serverId})
        val admin=owner.copy(role=UserRole.ADMIN)
        assertEquals(3,repo.getOrders(admin).orders.size);assertEquals(key,api.lastKey)
        val noKey=OrderRepository(db,api).getOrders(admin)
        assertFalse(noKey.remoteFresh);assertNotNull(noKey.error)
    }
    @Test fun statusAdvanceRequiresAdminAndKeyAndClientRefreshPersistsNewServerState()=runBlocking {
        val owner=user();val repo=OrderRepository(db,api) {key}
        repo.placeOrder(owner,DeliveryMethod.PICKUP,prepare(owner))
        val order=db.readOrders(owner.id).single();val admin=owner.copy(role=UserRole.ADMIN)
        try {repo.advanceStatus(owner,order);fail("client advanced")} catch(_:IllegalArgumentException) {}
        try {OrderRepository(db,api).advanceStatus(admin,order);fail("missing key")} catch(_:IllegalStateException) {}
        repo.advanceStatus(admin,order)
        assertEquals(OrderStatus.CONFIRMED,repo.getOrders(owner).orders.single().status)
        assertEquals(OrderStatus.CONFIRMED,db.readOrders(owner.id).single().status)
        db.setCartQuantity(owner.id,1,1)
        val legacyId=db.placeDemoOrder(owner,DeliveryMethod.PICKUP,db.readCart(owner.id))
        val legacy=db.readOrders(owner.id).single {it.id==legacyId}
        repo.advanceStatus(admin,legacy)
        assertEquals(OrderStatus.CONFIRMED,db.readOrders(owner.id).single {it.id==legacyId}.status)
    }
    @Test fun offlineAndHttpHistoryUseCacheButCancellationPropagates()=runBlocking {
        val owner=user();val repo=OrderRepository(db,api)
        repo.placeOrder(owner,DeliveryMethod.PICKUP,prepare(owner))
        val local=db.readOrders(owner.id)
        for(failure in listOf(java.io.IOException(),LogicFixtures.http(404),IllegalStateException("bad response"))) {
            api.readFailure=failure;val history=repo.getOrders(owner)
            assertFalse(history.remoteFresh);assertEquals(local,history.orders)
            assertTrue(history.error!!.contains("desactualizados"))
        }
        api.readFailure=CancellationException()
        try {repo.getOrders(owner);fail("cancelled")} catch(_:CancellationException) {}
    }
    @Test fun invalidRoleUnmappedCartAndConnectionFailureNeverCreateOrders()=runBlocking {
        val owner=user();val repo=OrderRepository(db,api)
        try {repo.placeOrder(owner.copy(role=UserRole.ADMIN),DeliveryMethod.PICKUP,emptyList());fail("admin purchase")} catch(_:IllegalArgumentException) {}
        val local=db.saveProduct(null,LogicFixtures.draft.copy(name="Local sin vínculo"))
        db.setCartQuantity(owner.id,local,1)
        try {repo.placeOrder(owner,DeliveryMethod.PICKUP,db.readCart(owner.id));fail("local-only cart")} catch(_:IllegalArgumentException) {}
        api.productsFailure=java.io.IOException()
        try {repo.placeOrder(owner,DeliveryMethod.PICKUP,db.readCart(owner.id));fail("offline")} catch(_:java.io.IOException) {}
        assertTrue(api.orders.isEmpty());assertTrue(api.submittedJson.isEmpty());assertNull(db.readPendingCheckout(owner.id))
    }
}
