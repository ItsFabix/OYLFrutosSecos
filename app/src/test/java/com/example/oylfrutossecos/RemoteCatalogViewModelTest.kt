package com.example.oylfrutossecos

import android.content.Context
import android.content.SharedPreferences
import com.example.oylfrutossecos.data.remote.*
import com.example.oylfrutossecos.ui.viewmodel.RemoteCatalogViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteCatalogViewModelTest : LogicViewModelTest() {
    private val service = mockk<OylRestService>()
    private val context = mockk<Context>()
    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>()
    private val persisted = mutableMapOf<String, String>()
    private var commitSucceeds = true
    private val health = ApiHealth("UP", "oyl", "rest")
    private val product = ApiProduct(1, "Almendras", "Frutos secos", "Bolsa seleccionada", 3000, 250, 5, "nut")
    private val order = ApiOrderResponse("server-order", "request", "cliente-android-demo", "PENDING", 3000,
        listOf(ApiOrderItem(1, "Almendras", 1, 3000)))

    private fun model(): RemoteCatalogViewModel {
        every { context.applicationContext } returns context
        every { context.getSharedPreferences("oyl_api_demo_order", Context.MODE_PRIVATE) } returns preferences
        every { preferences.getString(any(), any()) } answers { persisted[firstArg()] ?: secondArg() }
        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            if (commitSucceeds) persisted[firstArg()] = secondArg()
            editor
        }
        every { editor.remove(any()) } answers { if (commitSucceeds) persisted.remove(firstArg()); editor }
        every { editor.commit() } answers { commitSucceeds }
        return create(RemoteCatalogViewModel.Factory(service, context), RemoteCatalogViewModel::class.java)
    }

    @Test fun loadShowsProgressPreventsDuplicateQueriesAndRecoversAfterConnectionError() = runTest(main) {
        val gate = CompletableDeferred<ApiHealth>()
        coEvery { service.health() } coAnswers { gate.await() }
        coEvery { service.products() } returns listOf(product)
        val vm = model(); runCurrent()
        assertTrue(vm.state.value.loading)
        vm.load(); vm.createDemoOrder(); runCurrent()
        coVerify(exactly = 1) { service.health() }
        coVerify(exactly = 0) { service.createOrder(any()) }
        gate.complete(health); advanceUntilIdle()
        assertFalse(vm.state.value.loading); assertEquals(health, vm.state.value.health)
        assertEquals(listOf(product), vm.state.value.products)
        coEvery { service.health() } throws java.io.IOException("sin conexión")
        vm.load(); advanceUntilIdle()
        assertEquals("No pudimos establecer una conexión. Inténtalo nuevamente.", vm.state.value.error)
        assertEquals(listOf(product), vm.state.value.products)
        coEvery { service.health() } returns health
        vm.load(); advanceUntilIdle(); assertNull(vm.state.value.error)
    }

    @Test fun failedOrderRetainsRequestAcrossViewModelRecreationAndSuccessRefreshesStock() = runTest(main) {
        coEvery { service.health() } returns health
        coEvery { service.products() } returns listOf(product)
        val requests = mutableListOf<ApiOrderRequest>()
        coEvery { service.createOrder(capture(requests)) } throws java.io.IOException("respuesta perdida")
        val first = model(); advanceUntilIdle()
        first.prepareAnotherDemoOrder(); assertNull(first.state.value.requestId)
        first.createDemoOrder(); advanceUntilIdle()
        val requestId = first.state.value.requestId!!
        assertEquals(requestId, persisted["request_id"])
        assertNull(first.state.value.order); assertFalse(first.state.value.creatingOrder)
        assertTrue(first.state.value.orderError!!.contains("misma solicitud"))
        val recreated = model(); advanceUntilIdle(); assertEquals(requestId, recreated.state.value.requestId)
        coEvery { service.createOrder(capture(requests)) } returns order.copy(requestId = requestId)
        coEvery { service.products() } returns listOf(product.copy(stock = 4))
        recreated.createDemoOrder(); advanceUntilIdle()
        assertEquals(requests.first(), requests.last())
        assertEquals(listOf(ApiOrderItemRequest(1, 1)), requests.last().items)
        assertEquals("cliente-android-demo", requests.last().customerAlias)
        assertEquals(4, recreated.state.value.products.single().stock)
        assertNotNull(recreated.state.value.order); assertNull(recreated.state.value.orderError)
        commitSucceeds = false; recreated.prepareAnotherDemoOrder()
        assertNotNull(recreated.state.value.order)
        commitSucceeds = true; recreated.prepareAnotherDemoOrder()
        assertNull(recreated.state.value.order); assertNull(recreated.state.value.requestId)
        assertFalse(persisted.containsKey("request_id"))
    }

    @Test fun persistenceFailureNeverSendsOrderAndBusyOrderCannotBeRepeatedOrReset() = runTest(main) {
        coEvery { service.health() } returns health
        coEvery { service.products() } returns listOf(product)
        val vm = model(); advanceUntilIdle(); commitSucceeds = false
        vm.createDemoOrder(); advanceUntilIdle()
        assertTrue(vm.state.value.orderError!!.contains("guardar la solicitud"))
        coVerify(exactly = 0) { service.createOrder(any()) }
        commitSucceeds = true
        val gate = CompletableDeferred<ApiOrderResponse>()
        coEvery { service.createOrder(any()) } coAnswers { gate.await() }
        vm.createDemoOrder(); runCurrent(); assertTrue(vm.state.value.creatingOrder)
        vm.createDemoOrder(); vm.prepareAnotherDemoOrder(); runCurrent()
        coVerify(exactly = 1) { service.createOrder(any()) }
        assertNotNull(vm.state.value.requestId)
        coEvery { service.products() } throws java.io.IOException("stock unavailable")
        gate.complete(order); advanceUntilIdle()
        assertEquals(order, vm.state.value.order); assertFalse(vm.state.value.creatingOrder)
        assertNotNull(vm.state.value.catalogWarning); assertNull(vm.state.value.orderError)
    }

    @Test fun unavailableServerAndCancellationNeverInventOrderConfirmation() = runTest(main) {
        coEvery { service.health() } throws RuntimeException()
        val vm = model(); advanceUntilIdle()
        assertEquals("No pudimos completar la operación. Inténtalo nuevamente.", vm.state.value.error)
        vm.createDemoOrder(); advanceUntilIdle()
        coVerify(exactly = 0) { service.createOrder(any()) }
        coEvery { service.health() } returns health
        coEvery { service.products() } returns listOf(product)
        vm.load(); advanceUntilIdle()
        coEvery { service.createOrder(any()) } throws CancellationException("leaving screen")
        vm.createDemoOrder(); advanceUntilIdle()
        assertNull(vm.state.value.order); assertNull(vm.state.value.orderError)
        assertNotNull(persisted["request_id"])
    }

    @Test fun cancellationWhileRefreshingConfirmedOrderDoesNotTurnItIntoFailedPurchase() = runTest(main) {
        coEvery { service.health() } returns health
        coEvery { service.products() } returns listOf(product)
        coEvery { service.createOrder(any()) } returns order
        val vm = model(); advanceUntilIdle()
        coEvery { service.products() } throws CancellationException()
        vm.createDemoOrder(); advanceUntilIdle()
        assertEquals(order, vm.state.value.order); assertNull(vm.state.value.orderError)
        assertNull(vm.state.value.catalogWarning)
    }
}
