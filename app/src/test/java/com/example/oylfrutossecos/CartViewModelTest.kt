package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.local.PendingCheckout
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.CartRepository
import com.example.oylfrutossecos.ui.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest : LogicViewModelTest() {
    private val repo=mockk<CartRepository>()
    private val owner=LogicFixtures.client.id
    private fun vm()=create(CartViewModel.Factory(repo,owner),CartViewModel::class.java)
    @Test fun loadingSnapshotTotalsAndPendingLockMatchThePersistedUserCart() = runTest {
        val gate=CompletableDeferred<CartRepository.Snapshot>()
        coEvery { repo.snapshot(owner) } coAnswers { gate.await() }
        val vm=vm();assertTrue(vm.state.value.loading);assertFalse(vm.state.value.canEdit)
        vm.change(41,3);coVerify(exactly=0) { repo.setQuantity(any(),any(),any()) }
        runCurrent();gate.complete(CartRepository.Snapshot(LogicFixtures.cart,null));advanceUntilIdle()
        assertEquals(6000,vm.state.value.total);assertEquals(2,vm.state.value.count)
        assertTrue(vm.state.value.canCheckout);assertTrue(vm.state.value.canEdit)
        val pending=PendingCheckout("req","{}",DeliveryMethod.PICKUP,"1:2")
        coEvery { repo.snapshot(owner) } returns CartRepository.Snapshot(LogicFixtures.cart,pending)
        vm.reload();advanceUntilIdle();assertEquals(pending,vm.state.value.pending)
        assertFalse(vm.state.value.canCheckout);assertFalse(vm.state.value.canEdit)
        vm.add(41,1);coVerify(exactly=0) { repo.setQuantity(any(),any(),any()) }
    }
    @Test fun addUsesExistingQuantityAndEmptyReloadRemovesConfirmation() = runTest {
        coEvery { repo.snapshot(owner) } returns CartRepository.Snapshot(LogicFixtures.cart,null)
        coEvery { repo.setQuantity(owner,41,3) } returns Unit
        coEvery { repo.getCart(owner) } returns listOf(CartItem(LogicFixtures.product,3))
        val vm=vm();advanceUntilIdle();vm.add(41,1)
        assertTrue(vm.state.value.busy);vm.change(41,4);advanceUntilIdle()
        assertEquals(3,vm.state.value.count);assertEquals(9000,vm.state.value.total)
        assertEquals("Producto agregado al carrito.",vm.state.value.notice)
        coVerify(exactly=1) { repo.setQuantity(owner,41,3) }
        coEvery { repo.snapshot(owner) } returns CartRepository.Snapshot(emptyList(),null)
        vm.reload();advanceUntilIdle();assertNull(vm.state.value.notice);assertFalse(vm.state.value.canCheckout)
        vm.clearMessage();assertNull(vm.state.value.error)
    }
    @Test fun addNewLineValidationFailureAndUnexpectedErrorKeepCartUnchanged() = runTest {
        coEvery { repo.snapshot(owner) } returns CartRepository.Snapshot(LogicFixtures.cart,null)
        val vm=vm();advanceUntilIdle()
        coEvery { repo.setQuantity(owner,42,1) } throws IllegalArgumentException("Sin stock")
        vm.add(42,1);advanceUntilIdle();assertEquals("Sin stock",vm.state.value.error)
        assertEquals(LogicFixtures.cart,vm.state.value.items);assertFalse(vm.state.value.busy)
        coEvery { repo.setQuantity(owner,41,0) } throws java.io.IOException()
        vm.change(41,0);advanceUntilIdle();assertTrue(vm.state.value.error!!.contains("actualizar"))
        vm.clearMessage();assertNull(vm.state.value.error);assertNull(vm.state.value.notice)
    }
    @Test fun sessionOrPendingConflictReloadsSnapshotRatherThanLeavingEditableStaleCart() = runTest {
        coEvery { repo.snapshot(owner) } returns CartRepository.Snapshot(LogicFixtures.cart,null)
        val vm=vm();advanceUntilIdle()
        val pending=PendingCheckout("same","{}",DeliveryMethod.DELIVERY,"1:2")
        coEvery { repo.setQuantity(any(),any(),any()) } throws IllegalStateException("Checkout pendiente")
        coEvery { repo.snapshot(owner) } returns CartRepository.Snapshot(LogicFixtures.cart,pending)
        vm.change(41,1);advanceUntilIdle()
        assertEquals(pending,vm.state.value.pending);assertFalse(vm.state.value.canEdit)
        coVerify(exactly=2) { repo.snapshot(owner) }
    }
    @Test fun failedReadIsVisibleAndCancellationDoesNotBecomeOrdinaryError() = runTest {
        coEvery { repo.snapshot(owner) } throws java.io.IOException()
        val vm=vm();advanceUntilIdle();assertFalse(vm.state.value.loading)
        assertEquals("No se pudo leer el carrito.",vm.state.value.error)
        coEvery { repo.snapshot(owner) } throws CancellationException()
        vm.reload();advanceUntilIdle();assertNull(vm.state.value.error)
        coEvery { repo.snapshot(owner) } returns CartRepository.Snapshot(LogicFixtures.cart,null)
        vm.reload();advanceUntilIdle()
        coEvery { repo.setQuantity(any(),any(),any()) } throws CancellationException()
        vm.change(41,1);advanceUntilIdle();assertNull(vm.state.value.error)
        assertEquals(LogicFixtures.cart,vm.state.value.items)
    }
}
