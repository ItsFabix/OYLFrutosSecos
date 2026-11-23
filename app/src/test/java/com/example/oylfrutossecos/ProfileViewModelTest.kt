package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.domain.ProfileValidator
import com.example.oylfrutossecos.ui.viewmodel.ProfileViewModel
import java.io.IOException
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ProfileViewModelTest {
    private val owner = User(7, "Cliente Uno", "cliente", "cliente@example.cl", "912345678", "Calle de Prueba 123", UserRole.CLIENT)
    private val valid = ProfileDraft("Nombre Nuevo", "+56 9 8765-4321", "Dirección nueva 456")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    @After fun close() { scope.cancel() }

    @Test fun validationRejectsEmptyShortAndOversizedFieldsAndInvalidPhones() {
        listOf(ProfileDraft(), valid.copy(name = "X", phone = "123", address = "X"),
            valid.copy(name = "a".repeat(81), phone = "9999999999", address = "a".repeat(161))).forEach {
            assertEquals(setOf("name", "phone", "address"), ProfileValidator.validate(it).keys)
        }
        assertTrue(ProfileValidator.validate(valid).isEmpty())
        assertTrue(ProfileValidator.validate(valid.copy(phone = "912345678")).isEmpty())
    }
    @Test fun invalidFormNeverWritesAndCorrectingAFieldClearsOnlyItsError() {
        var writes = 0
        val vm = ProfileViewModel(owner, { _, _ -> writes++; owner }, scope)
        vm.change(ProfileDraft())
        vm.save { fail("invalid form saved") }
        assertEquals(0, writes)
        vm.change(ProfileDraft(name = "Nombre válido"))
        assertEquals(setOf("phone", "address"), vm.state.value.errors.keys)
    }
    @Test fun saveUsesOriginalOwnerAndIgnoresDoubleTapWhileBusyOrSaved() {
        var writes = 0
        val gate = CompletableDeferred<User>()
        val vm = ProfileViewModel(owner, { id, draft ->
            assertEquals(owner.id, id); assertEquals(valid, draft); writes++; gate.await()
        }, scope)
        var result: User? = null
        vm.change(valid)
        vm.save { result = it }; vm.save { fail("duplicate save") }
        assertTrue(vm.state.value.busy)
        vm.change(valid.copy(name = "Otro"))
        assertEquals(valid, vm.state.value.draft)
        val updated = owner.copy(name = valid.name, phone = valid.phone, address = valid.address)
        gate.complete(updated)
        assertEquals(updated, result)
        assertTrue(vm.state.value.saved)
        vm.save { fail("already saved") }
        assertEquals(1, writes)
    }
    @Test fun storageErrorKeepsDraftAndAllowsRetryWithoutExposingException() {
        var failWrite = true
        val vm = ProfileViewModel(owner, { _, draft ->
            if (failWrite) throw IOException("private database details")
            owner.copy(name = draft.name, phone = draft.phone, address = draft.address)
        }, scope)
        vm.change(valid)
        vm.save { fail("write failed") }
        assertEquals(valid, vm.state.value.draft)
        assertFalse(vm.state.value.busy)
        assertFalse(vm.state.value.error!!.contains("private"))
        failWrite = false
        vm.save { }
        assertTrue(vm.state.value.saved)
    }
    @Test fun changedSessionOrIdentityIsRejectedWithoutSuccessCallback() {
        listOf(owner.copy(id = 8), owner.copy(role = UserRole.ADMIN), owner.copy(alias = "other"),
            owner.copy(email = "other@example.cl")).forEach { unexpected ->
            val vm = ProfileViewModel(owner, { _, _ -> unexpected }, scope)
            vm.change(valid)
            vm.save { fail("identity changed") }
            assertFalse(vm.state.value.saved)
            assertNotNull(vm.state.value.error)
        }
    }
}
