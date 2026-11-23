package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.remote.*
import com.example.oylfrutossecos.ui.viewmodel.AdminSessionViewModel
import java.io.IOException
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AdminSessionViewModelTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val candidate = "test-only-administrative-key"
    @After fun close() { scope.cancel() }

    private fun http(code: Int) = HttpException(Response.error<Any>(code,
        ResponseBody.create(null, "Sensitive server detail $candidate")))

    @Test fun connectsOnlyAfterBothProtectedReadsAndRefreshesOnce() {
        val gate = CompletableDeferred<Unit>()
        val vm = AdminSessionViewModel(scope)
        var refreshed = 0
        val api = object : StubOylRestService() {
            override suspend fun adminOrders(key: String): List<ApiOrderResponse> {
                assertEquals(candidate, key)
                assertFalse(vm.configured)
                return emptyList()
            }
            override suspend fun inventoryMovements(key: String): List<ApiInventoryMovement> {
                assertEquals(candidate, key)
                gate.await()
                return emptyList()
            }
        }
        vm.connect(candidate, api) { refreshed++ }
        assertTrue(vm.validating)
        assertEquals("", vm.key())
        vm.connect("another-test-credential", api) { fail("Duplicate submission") }
        gate.complete(Unit)
        assertTrue(vm.configured)
        assertFalse(vm.validating)
        assertEquals(candidate, vm.key())
        assertEquals(1, refreshed)
        vm.clear()
        assertFalse(vm.configured)
        assertEquals("", vm.key())
        assertFalse(AdminSessionViewModel(scope).configured)
    }

    @Test fun forbiddenIsDistinctFromNetworkAndServerErrorsWithoutLeakingSecrets() {
        for (failure in listOf(http(403), IOException("Network detail $candidate"), http(500))) {
            val vm = AdminSessionViewModel(scope)
            val api = object : StubOylRestService() {
                override suspend fun adminOrders(key: String): List<ApiOrderResponse> = throw failure
            }
            vm.connect(candidate, api) { fail("Failure must not refresh or authenticate") }
            assertFalse(vm.configured)
            assertFalse(vm.validating)
            assertEquals("", vm.key())
            assertNotNull(vm.error)
            assertFalse(vm.error!!.contains(candidate))
            when (failure) {
                is IOException -> assertTrue(vm.error!!.contains("No se pudo contactar"))
                is HttpException -> if (failure.code() == 403) assertTrue(vm.error!!.contains("rechazó la clave"))
                    else assertTrue(vm.error!!.contains("HTTP 500"))
            }
        }
    }

    @Test fun inventoryForbiddenDoesNotLeaveAnAuthenticatedSession() {
        val vm = AdminSessionViewModel(scope)
        val api = object : StubOylRestService() {
            override suspend fun adminOrders(key: String) = emptyList<ApiOrderResponse>()
            override suspend fun inventoryMovements(key: String): List<ApiInventoryMovement> = throw http(403)
        }
        vm.connect(candidate, api) { fail("Both endpoints must authorize") }
        assertFalse(vm.configured)
        assertEquals("", vm.key())
    }

    @Test fun rejectionClearsCurrentCredentialButLateRejectionDoesNotClearReplacement() {
        val vm = AdminSessionViewModel(scope)
        val api = object : StubOylRestService() {
            override suspend fun adminOrders(key: String) = emptyList<ApiOrderResponse>()
            override suspend fun inventoryMovements(key: String) = emptyList<ApiInventoryMovement>()
        }
        vm.connect(candidate, api) { }
        val replacement = "replacement-test-key-only"
        vm.connect(replacement, api) { }
        vm.reject(candidate)
        assertTrue(vm.configured)
        assertEquals(replacement, vm.key())
        vm.reject(replacement)
        assertFalse(vm.configured)
        assertEquals("", vm.key())
        assertNull(vm.error)
    }

    @Test fun clearCancelsValidationAndLateResponseCannotRestoreCredential() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val vm = AdminSessionViewModel(scope)
        val api = object : StubOylRestService() {
            override suspend fun adminOrders(key: String): List<ApiOrderResponse> {
                withContext(NonCancellable) { gate.await() }
                return emptyList()
            }
            override suspend fun inventoryMovements(key: String) = emptyList<ApiInventoryMovement>()
        }
        vm.connect(candidate, api) { fail("Disposed session must not refresh") }
        vm.clear()
        gate.complete(Unit)
        yield()
        assertFalse(vm.configured)
        assertFalse(vm.validating)
        assertEquals("", vm.key())
        assertNull(vm.error)
    }
}
