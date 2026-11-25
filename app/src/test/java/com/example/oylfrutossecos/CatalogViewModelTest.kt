package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.repository.ProductRepository
import com.example.oylfrutossecos.ui.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest : LogicViewModelTest() {
    private val repo = mockk<ProductRepository>()
    private val products = listOf(LogicFixtures.product, LogicFixtures.product.copy(id=42,name="Chía",category="Semillas"))
    private fun vm() = create(CatalogViewModel.Factory(repo), CatalogViewModel::class.java)

    @Test fun loadsAndFiltersCatalogByQueryCategoryAndClearsBoth() = runTest {
        coEvery { repo.getProducts() } returns products
        val vm=vm()
        assertTrue(vm.state.value.loading)
        advanceUntilIdle()
        assertEquals(products,vm.state.value.products)
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.savingProduct)
        assertEquals(listOf("Frutos secos","Semillas"),vm.state.value.categories)
        vm.search("chía"); assertEquals("chía",vm.state.value.query)
        vm.selectCategory("Semillas"); assertEquals("Semillas",vm.state.value.category)
        assertEquals(listOf(products[1]),vm.state.value.visibleProducts)
        vm.clearFilters(); assertEquals(products,vm.state.value.visibleProducts)
        assertEquals("",vm.state.value.query); assertNull(vm.state.value.category)
    }
    @Test fun failureUsesCachedProductsAndWarnsThatStockMayHaveChanged() = runTest {
        coEvery { repo.getProducts() } throws java.io.IOException("offline")
        coEvery { repo.cachedProducts() } returns products
        val vm=vm(); advanceUntilIdle()
        assertFalse(vm.state.value.loading); assertEquals(products,vm.state.value.products)
        assertTrue(vm.state.value.error!!.contains("última información guardada"))
        coEvery { repo.getProducts() } returns products.take(1)
        vm.load(); advanceUntilIdle()
        assertNull(vm.state.value.error); assertEquals(1,vm.state.value.products.size)
    }
    @Test fun saveBlocksDoubleSubmitAndRefreshesOnlyAfterConfirmation() = runTest {
        coEvery { repo.getProducts() } returns products
        coEvery { repo.cachedProducts() } returns products.take(1)
        val gate=CompletableDeferred<Long>()
        coEvery { repo.saveProduct(41,LogicFixtures.draft) } coAnswers { gate.await() }
        val vm=vm(); advanceUntilIdle()
        val results=mutableListOf<String?>()
        vm.saveProduct(41,LogicFixtures.draft) { results+=it }
        assertTrue(vm.state.value.savingProduct)
        vm.saveProduct(41,LogicFixtures.draft) { results+=it }
        runCurrent(); assertEquals(1,results.size); assertNotNull(results[0])
        gate.complete(41); advanceUntilIdle()
        assertEquals(listOf(products[0]),vm.state.value.products)
        assertNull(results.last()); assertFalse(vm.state.value.savingProduct)
        coVerify(exactly=1) { repo.saveProduct(41,LogicFixtures.draft) }
    }
    @Test fun invalidDraftNeverWritesAndHttpFailureKeepsExistingCatalog() = runTest {
        coEvery { repo.getProducts() } returns products
        val vm=vm(); advanceUntilIdle()
        var error:String?=null
        vm.saveProduct(null,LogicFixtures.draft.copy(priceClp=0)) { error=it }
        assertNotNull(error); coVerify(exactly=0) { repo.saveProduct(any(),any()) }
        coEvery { repo.saveProduct(null,LogicFixtures.draft) } throws LogicFixtures.http(403)
        vm.saveProduct(null,LogicFixtures.draft) { error=it }; advanceUntilIdle()
        assertTrue(error!!.contains("administrativa")); assertEquals(products,vm.state.value.products)
        assertFalse(vm.state.value.savingProduct)
    }
    @Test fun cancellationNeverReportsAStorageErrorOrSuccess() = runTest {
        coEvery { repo.getProducts() } returns emptyList()
        val vm=vm(); advanceUntilIdle()
        coEvery { repo.saveProduct(any(),any()) } throws CancellationException("test cancellation")
        vm.saveProduct(null,LogicFixtures.draft) { fail("cancelled save callback") }; advanceUntilIdle()
        assertFalse(vm.state.value.savingProduct); assertNull(vm.state.value.error)
        coEvery { repo.getProducts() } throws CancellationException()
        vm.load(); advanceUntilIdle()
        coVerify(exactly=0) { repo.cachedProducts() }
    }
}
