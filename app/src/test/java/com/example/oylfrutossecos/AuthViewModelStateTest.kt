package com.example.oylfrutossecos

import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.AliasAlreadyUsedException
import com.example.oylfrutossecos.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class AuthViewModelStateTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val valid = Registration("Cliente Demo", "cliente1", "cliente1@example.com",
        "+56 9 1234 5678", "Calle de Prueba 123", "Cliente123", "Cliente123")
    private val client = User(8, valid.name, valid.alias, valid.email, valid.phone, valid.address, UserRole.CLIENT)

    private inner class Fixture {
        var registerCalls = 0
        var loginCalls = 0
        var registerGate: CompletableDeferred<Unit>? = null
        var loginGate: CompletableDeferred<Unit>? = null
        var registerError: Exception? = null
        var restoredUser: User? = null
        var loginResult: User? = client
        var registered: Registration? = null
        var credentials: Triple<String, String, UserRole>? = null
        fun vm() = AuthViewModel(
            readCurrentUser = { restoredUser },
            saveRegistration = {
                registerCalls++
                registered = it
                registerGate?.await()
                registerError?.let { failure -> throw failure }
            },
            verifyCredentials = { alias, password, role ->
                loginCalls++
                credentials = Triple(alias, password, role)
                loginGate?.await()
                loginResult
            },
            clearCurrentSession = {}, operationScope = scope)
    }

    @After fun cancelOnlyTestOperations() { scope.cancel() }

    @Test fun blankRegistrationReportsAllSevenFieldsWithoutCallingPersistence() {
        val fixture = Fixture()
        val vm = fixture.vm()
        vm.register()
        assertEquals(setOf("name", "alias", "email", "phone", "address", "password", "confirmation"),
            vm.registerState.errors.keys)
        assertEquals(0, fixture.registerCalls)
        assertFalse(vm.registerState.busy)
    }

    @Test fun changingEachRegistrationFieldClearsOnlyItsOwnError() {
        val changes = mapOf<String, (Registration) -> Registration>(
            "name" to { it.copy(name = valid.name) }, "alias" to { it.copy(alias = valid.alias) },
            "email" to { it.copy(email = valid.email) }, "phone" to { it.copy(phone = valid.phone) },
            "address" to { it.copy(address = valid.address) }, "password" to { it.copy(password = valid.password) },
            "confirmation" to { it.copy(confirmation = valid.confirmation) })
        changes.forEach { (field, change) ->
            val vm = Fixture().vm()
            vm.register()
            val before = vm.registerState.errors
            vm.changeRegistration(change(vm.registerState.form))
            assertEquals(field, before - field, vm.registerState.errors)
        }
    }

    @Test fun aliasEmailAndPasswordCanBeCorrectedIndependently() {
        val vm = Fixture().vm()
        vm.changeRegistration(valid.copy(alias = "?", email = "mal", password = "x", confirmation = ""))
        vm.register()
        vm.changeRegistration(vm.registerState.form.copy(alias = valid.alias))
        assertEquals(setOf("email", "password", "confirmation"), vm.registerState.errors.keys)
        vm.changeRegistration(vm.registerState.form.copy(email = valid.email))
        assertEquals(setOf("password", "confirmation"), vm.registerState.errors.keys)
        vm.changeRegistration(vm.registerState.form.copy(password = valid.password))
        assertEquals(setOf("confirmation"), vm.registerState.errors.keys)
    }

    @Test fun unchangedRegistrationDoesNotClearAnyErrors() {
        val vm = Fixture().vm()
        vm.register()
        val before = vm.registerState
        vm.changeRegistration(before.form)
        assertEquals(before, vm.registerState)
    }

    @Test fun changingSeveralFieldsOnlyClearsTheChangedSet() {
        val vm = Fixture().vm()
        vm.register()
        val before = vm.registerState.errors
        vm.changeRegistration(vm.registerState.form.copy(name = valid.name, email = valid.email))
        assertEquals(before - setOf("name", "email"), vm.registerState.errors)
    }

    @Test fun differentConfirmationBlocksRegistrationUntilConfirmationIsCorrected() {
        val fixture = Fixture()
        val vm = fixture.vm()
        vm.changeRegistration(valid.copy(confirmation = "Distinta123"))
        vm.register()
        assertEquals(setOf("confirmation"), vm.registerState.errors.keys)
        assertEquals(0, fixture.registerCalls)
        vm.changeRegistration(vm.registerState.form.copy(confirmation = valid.password))
        vm.register()
        assertEquals(valid, fixture.registered)
        assertTrue(vm.registerState.completed)
        assertEquals(valid.alias, vm.loginState.alias)
        assertNotNull(vm.loginState.successMessage)
        assertEquals("", vm.registerState.form.password)
    }

    @Test fun changingPasswordStillRequiresConfirmationToMatchOnSubmit() {
        val fixture = Fixture()
        val vm = fixture.vm()
        vm.changeRegistration(valid)
        vm.changeRegistration(valid.copy(password = "Nueva1234"))
        vm.register()
        assertEquals(setOf("confirmation"), vm.registerState.errors.keys)
        assertEquals(0, fixture.registerCalls)
    }

    @Test fun duplicateAliasAppearsAtAliasAndKeepsEveryEnteredValue() {
        val fixture = Fixture().apply { registerError = AliasAlreadyUsedException() }
        val vm = fixture.vm()
        vm.changeRegistration(valid)
        vm.register()
        assertEquals(valid, vm.registerState.form)
        assertEquals(mapOf("alias" to "Este alias ya está registrado"), vm.registerState.errors)
        vm.changeRegistration(valid.copy(name = "Otro Nombre"))
        assertTrue(vm.registerState.errors.containsKey("alias"))
        vm.changeRegistration(vm.registerState.form.copy(alias = "cliente2"))
        assertTrue(vm.registerState.errors.isEmpty())
        assertEquals(valid.email, vm.registerState.form.email)
        assertEquals(valid.password, vm.registerState.form.password)
        fixture.registerError = null
        vm.register()
        assertTrue(vm.registerState.completed)
        assertEquals(2, fixture.registerCalls)
    }

    @Test fun editingLoginAliasKeepsPasswordErrorAndViceVersa() {
        val vm = Fixture().vm()
        vm.login()
        vm.changeLogin(alias = valid.alias)
        assertEquals(setOf("password"), vm.loginState.errors.keys)
        vm.changeLogin(password = valid.password)
        assertTrue(vm.loginState.errors.isEmpty())
        vm.clearForms()
        vm.login()
        vm.changeLogin(password = valid.password)
        assertEquals(setOf("alias"), vm.loginState.errors.keys)
    }

    @Test fun roleChangesAndNoOpChangesKeepLoginFieldErrors() {
        val vm = Fixture().vm()
        vm.login()
        val errors = vm.loginState.errors
        vm.changeLogin(role = UserRole.ADMIN)
        assertEquals(errors, vm.loginState.errors)
        assertEquals(UserRole.ADMIN, vm.loginState.role)
        val before = vm.loginState
        vm.changeLogin()
        assertEquals(before, vm.loginState)
    }

    @Test fun wrongCredentialsKeepValuesAndTheExistingMessage() {
        val fixture = Fixture().apply { loginResult = null }
        val vm = fixture.vm()
        vm.changeLogin(valid.alias, valid.password, UserRole.ADMIN)
        vm.login()
        assertEquals("Revisa el alias, la contraseña y el tipo de usuario.", vm.loginState.errorMessage)
        assertEquals(valid.alias, vm.loginState.alias)
        assertEquals(valid.password, vm.loginState.password)
        assertEquals(UserRole.ADMIN, vm.loginState.role)
        vm.changeLogin()
        assertNotNull(vm.loginState.errorMessage)
        vm.changeLogin(role = UserRole.CLIENT)
        assertNull(vm.loginState.errorMessage)
    }

    @Test fun rapidRegisterTapsCannotEnqueueTwoWritesEvenAfterCompletion() {
        val gate = CompletableDeferred<Unit>()
        val fixture = Fixture().apply { registerGate = gate }
        val vm = fixture.vm()
        vm.changeRegistration(valid)
        vm.register(); vm.register()
        assertTrue(vm.registerState.busy)
        assertEquals(1, fixture.registerCalls)
        vm.changeRegistration(valid.copy(alias = "ignored"))
        vm.login()
        assertEquals(valid, vm.registerState.form)
        assertEquals(0, fixture.loginCalls)
        gate.complete(Unit)
        assertTrue(vm.registerState.completed)
        assertFalse(vm.registerState.busy)
        vm.register()
        assertEquals(1, fixture.registerCalls)
    }

    @Test fun rapidLoginTapsVerifyOnlyOnceAndClearSecretsAfterSuccess() {
        val gate = CompletableDeferred<Unit>()
        val fixture = Fixture().apply { loginGate = gate }
        val vm = fixture.vm()
        vm.changeLogin(valid.alias, valid.password, UserRole.CLIENT)
        vm.login(); vm.login()
        assertEquals(1, fixture.loginCalls)
        assertTrue(vm.loginState.busy)
        vm.changeLogin(alias = "ignored", password = "ignored")
        assertEquals(valid.alias, vm.loginState.alias)
        vm.register()
        assertEquals(0, fixture.registerCalls)
        gate.complete(Unit)
        assertEquals(client, vm.user)
        assertEquals(Triple(valid.alias, valid.password, UserRole.CLIENT), fixture.credentials)
        assertEquals("", vm.loginState.password)
        vm.login()
        assertEquals(1, fixture.loginCalls)
    }

    @Test fun registrationFailureUnlocksButtonsWithoutLosingValues() {
        val fixture = Fixture().apply { registerError = IllegalStateException("Test failure") }
        val vm = fixture.vm()
        vm.changeRegistration(valid)
        vm.register()
        assertFalse(vm.registerState.busy)
        assertNotNull(vm.registerState.errorMessage)
        assertEquals(valid, vm.registerState.form)
        fixture.registerError = null
        vm.register()
        assertEquals(2, fixture.registerCalls)
        assertTrue(vm.registerState.completed)
    }

    @Test fun restoringAnExistingUserPreservesItsRoleAndDoesNotRegisterAnotherAccount() {
        val existing = client.copy(role = UserRole.ADMIN)
        val fixture = Fixture().apply { restoredUser = existing }
        val vm = fixture.vm()
        assertEquals(existing, vm.user)
        vm.login(); vm.register()
        assertEquals(0, fixture.loginCalls)
        assertEquals(0, fixture.registerCalls)
    }
}
