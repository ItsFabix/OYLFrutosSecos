package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.*
import com.example.oylfrutossecos.ui.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest : LogicViewModelTest() {
    private val repo=mockk<OrderRepository>()
    private val user=LogicFixtures.client
    private val order=LogicFixtures.order
    private fun vm(owner:User=user)=create(OrderViewModel.Factory(repo,owner),OrderViewModel::class.java)

    @Test fun loadPublishesFreshAndCachedHistoriesAndCanRecoverFromException() = runTest {
        coEvery { repo.getOrders(user) } returns OrderHistory(listOf(order),"Última copia",false)
        val vm=vm(); assertTrue(vm.state.value.loading); advanceUntilIdle()
        assertEquals(listOf(order),vm.state.value.orders); assertFalse(vm.state.value.remoteFresh)
        assertEquals("Última copia",vm.state.value.error)
        coEvery { repo.getOrders(user) } throws java.io.IOException()
        vm.load(); advanceUntilIdle(); assertTrue(vm.state.value.error!!.contains("historial"))
        coEvery { repo.getOrders(user) } returns OrderHistory(listOf(order),remoteFresh=true)
        vm.load(); advanceUntilIdle(); assertTrue(vm.state.value.remoteFresh); assertNull(vm.state.value.error)
    }
    @Test fun checkoutDoubleTapIsBlockedAndSuccessAndSettledFireOnce() = runTest {
        coEvery { repo.getOrders(any()) } returns OrderHistory(listOf(order),remoteFresh=true)
        val gate=CompletableDeferred<PlacedRemoteOrder>()
        coEvery { repo.placeOrder(user,DeliveryMethod.PICKUP,LogicFixtures.cart) } coAnswers { gate.await() }
        val vm=vm(); advanceUntilIdle()
        var success=0;var settled=0
        vm.place(DeliveryMethod.PICKUP,LogicFixtures.cart,{settled++}) {success++}
        assertTrue(vm.state.value.busy); assertNull(vm.state.value.placedId)
        vm.place(DeliveryMethod.PICKUP,LogicFixtures.cart) {fail("duplicate")};vm.load()
        runCurrent();gate.complete(PlacedRemoteOrder(11,"remote-11"));advanceUntilIdle()
        assertEquals(1,success);assertEquals(1,settled);assertFalse(vm.state.value.busy)
        assertEquals(11L,vm.state.value.placedId);assertEquals("remote-11",vm.state.value.placedServerId)
        assertEquals(listOf(order),vm.state.value.orders)
        coVerify(exactly=1) { repo.placeOrder(user,DeliveryMethod.PICKUP,LogicFixtures.cart) }
    }
    @Test fun checkoutValidationStorageNetworkAndCancellationDoNotReportSuccess() = runTest {
        coEvery { repo.getOrders(any()) } returns OrderHistory(emptyList())
        for(failure in listOf(IllegalArgumentException("Carrito inválido"),IllegalStateException("Pendiente"),java.io.IOException(),CancellationException())) {
            val vm=vm();advanceUntilIdle();var settled=0
            coEvery { repo.placeOrder(any(),any(),any()) } throws failure
            vm.place(DeliveryMethod.PICKUP,LogicFixtures.cart,{settled++}) {fail("failed checkout")};advanceUntilIdle()
            assertFalse(vm.state.value.busy);assertNull(vm.state.value.placedId);assertEquals(1,settled)
            if(failure !is CancellationException) assertNotNull(vm.state.value.error)
            if(failure is java.io.IOException) assertTrue(vm.state.value.error!!.contains("No se confirmó"))
        }
    }
    @Test fun updatedContactIsUsedButIdentityAndAdminCheckoutAreRejected() = runTest {
        coEvery { repo.getOrders(any()) } returns OrderHistory(emptyList())
        coEvery { repo.placeOrder(any(),any(),any()) } returns PlacedRemoteOrder(12,"new")
        val vm=vm();advanceUntilIdle();val updated=user.copy(phone="987654321",address="Nueva dirección")
        vm.updateUser(updated);vm.place(DeliveryMethod.DELIVERY,LogicFixtures.cart) {};advanceUntilIdle()
        coVerify { repo.placeOrder(updated,DeliveryMethod.DELIVERY,LogicFixtures.cart) }
        for(other in listOf(updated.copy(id=99),updated.copy(alias="otro"),updated.copy(role=UserRole.ADMIN))) {
            assertThrows(IllegalArgumentException::class.java) { vm.updateUser(other) }
        }
        val admin=vm(LogicFixtures.admin);advanceUntilIdle()
        admin.place(DeliveryMethod.PICKUP,LogicFixtures.cart) {fail("admin purchase")}
        coVerify(exactly=1) { repo.placeOrder(any(),any(),any()) }
        vm.clearAdminSession();assertEquals(12L,vm.state.value.placedId)
    }
    @Test fun onlyAdminWithFreshRemoteHistoryCanAdvanceAndBusyBlocksDuplicate() = runTest {
        coEvery { repo.getOrders(any()) } returns OrderHistory(listOf(order),remoteFresh=true)
        val client=vm();advanceUntilIdle();client.advance(order)
        coVerify(exactly=0) { repo.advanceStatus(any(),any()) }
        val admin=vm(LogicFixtures.admin);advanceUntilIdle()
        val gate=CompletableDeferred<Unit>()
        coEvery { repo.advanceStatus(LogicFixtures.admin,order) } coAnswers { gate.await() }
        admin.advance(order);admin.advance(order);admin.load();runCurrent()
        assertTrue(admin.state.value.busy)
        coEvery { repo.getOrders(LogicFixtures.admin) } returns OrderHistory(listOf(order.copy(status=OrderStatus.CONFIRMED)),remoteFresh=true)
        gate.complete(Unit);advanceUntilIdle()
        assertEquals(OrderStatus.CONFIRMED,admin.state.value.orders.single().status)
        assertFalse(admin.state.value.busy)
        coVerify(exactly=1) { repo.advanceStatus(any(),any()) }
        admin.clearAdminSession();assertTrue(admin.state.value.orders.isEmpty());assertFalse(admin.state.value.loading)
        admin.advance(order);coVerify(exactly=1) { repo.advanceStatus(any(),any()) }
    }
    @Test fun advanceFailureDisablesFreshnessAndLegacyOrderCanAdvanceWithoutRemoteSnapshot() = runTest {
        coEvery { repo.getOrders(any()) } returns OrderHistory(listOf(order),remoteFresh=true)
        val admin=vm(LogicFixtures.admin);advanceUntilIdle()
        coEvery { repo.advanceStatus(any(),any()) } throws LogicFixtures.http(409)
        admin.advance(order);advanceUntilIdle()
        assertFalse(admin.state.value.remoteFresh);assertFalse(admin.state.value.busy)
        assertTrue(admin.state.value.error!!.contains("verificar"))
        coEvery { repo.advanceStatus(any(),any()) } throws IllegalStateException("Estado inválido")
        admin.advance(order.copy(remoteOrderId=null));advanceUntilIdle()
        assertEquals("Estado inválido",admin.state.value.error)
        coEvery { repo.getOrders(any()) } throws CancellationException()
        admin.load();advanceUntilIdle()
        assertTrue(admin.state.value.loading)
    }
}
