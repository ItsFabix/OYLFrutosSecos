package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.InventoryRepository
import com.example.oylfrutossecos.ui.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest : LogicViewModelTest() {
    private val repo=mockk<InventoryRepository>()
    private val movement=InventoryMovement(1,41,"Almendras",MovementType.ENTRY,2,3,5,"Reposición",100)
    private fun vm()=create(InventoryViewModel.Factory(repo,1),InventoryViewModel::class.java)
    private fun ready() {
        coEvery { repo.products() } returns listOf(LogicFixtures.product)
        coEvery { repo.movements(any()) } returns listOf(movement)
    }
    @Test fun loadCombinesServerProductsMovementsAndErrorCanBeRetried() = runTest {
        ready();val vm=vm();assertTrue(vm.state.value.loading);advanceUntilIdle()
        assertEquals(listOf(LogicFixtures.product),vm.state.value.products)
        assertEquals(listOf(movement),vm.state.value.movements);assertNull(vm.state.value.error)
        coEvery { repo.movements(any()) } throws LogicFixtures.http(403)
        vm.load();advanceUntilIdle();assertTrue(vm.state.value.error!!.contains("administrativa"))
        assertFalse(vm.state.value.loading);ready();vm.load();advanceUntilIdle();assertNull(vm.state.value.error)
    }
    @Test fun adjustmentBlocksDoubleTapAndLoadsUpdatedStockAfterSuccess() = runTest {
        ready();val gate=CompletableDeferred<Long>()
        coEvery { repo.adjust(1,41,MovementType.ENTRY,2,"Reposición") } coAnswers { gate.await() }
        val vm=vm();advanceUntilIdle();var success=0;val results=mutableListOf<String?>()
        vm.adjust(41,MovementType.ENTRY,2,"Reposición",{results+=it}) {success++}
        assertTrue(vm.state.value.busy)
        vm.adjust(41,MovementType.ENTRY,2,"Reposición",{results+=it}) {fail("duplicate")}
        vm.load();runCurrent();assertNotNull(results.single())
        coEvery { repo.products() } returns listOf(LogicFixtures.product.copy(stock=7))
        gate.complete(41);advanceUntilIdle()
        assertEquals(7,vm.state.value.products.single().stock);assertEquals(1,success)
        assertNull(results.last());assertFalse(vm.state.value.busy);assertNotNull(vm.state.value.notice)
        vm.clearNotice();assertNull(vm.state.value.notice);assertNull(vm.state.value.error)
        coVerify(exactly=1) { repo.adjust(any(),any(),any(),any(),any()) }
    }
    @Test fun validationStateAndNetworkErrorsReleaseBusyAndDoNotConfirm() = runTest {
        ready()
        for(failure in listOf(IllegalArgumentException("Cantidad inválida"),IllegalStateException("No autorizado"),java.io.IOException())) {
            val vm=vm();advanceUntilIdle();var error:String?=null
            coEvery { repo.adjust(any(),any(),any(),any(),any()) } throws failure
            vm.adjust(41,MovementType.EXIT,9,"Venta",{error=it}) {fail("failed movement")};advanceUntilIdle()
            assertNotNull(error);assertEquals(error,vm.state.value.error)
            assertFalse(vm.state.value.busy);assertNull(vm.state.value.notice)
        }
    }
    @Test fun clearCancelsPendingReadAndCancelledAdjustmentDoesNotClaimFailure() = runTest {
        ready();val gate=CompletableDeferred<List<com.example.oylfrutossecos.data.model.Product>>()
        coEvery { repo.products() } coAnswers { gate.await() }
        val vm=vm();runCurrent();vm.clearAdminSession();gate.complete(listOf(LogicFixtures.product));advanceUntilIdle()
        assertTrue(vm.state.value.products.isEmpty());assertTrue(vm.state.value.movements.isEmpty())
        assertFalse(vm.state.value.loading);assertNull(vm.state.value.error)
        coEvery { repo.adjust(any(),any(),any(),any(),any()) } throws CancellationException()
        vm.adjust(41,MovementType.ENTRY,1,"Test",{fail("cancelled callback")}) {fail("cancelled success")};advanceUntilIdle()
        assertFalse(vm.state.value.busy);assertNull(vm.state.value.error)
    }
}
