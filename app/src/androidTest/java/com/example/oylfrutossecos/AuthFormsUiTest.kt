package com.example.oylfrutossecos

import android.os.Bundle
import android.os.Parcel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.AliasAlreadyUsedException
import com.example.oylfrutossecos.ui.screens.LoginScreen
import com.example.oylfrutossecos.ui.screens.RegisterScreen
import com.example.oylfrutossecos.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Test activity and in-memory operations only: never opens the application database. */
class AuthFormsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var auth: AuthViewModel
    private val valid = Registration("Cliente Demo", "cliente1", "cliente1@example.cl",
        "912345678", "Calle de Prueba 123", "MemoryOnly1234", "MemoryOnly1234")
    private var registerAction: suspend (Registration) -> Unit = {}
    private var loginAction: suspend (String, String, UserRole) -> User? = { _, _, _ -> null }
    private val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel({ null }, { registerAction(it) },
                { a, p, r -> loginAction(a, p, r) }, {}) as T
        }
    }

    private fun render(registration: Boolean) {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val vm: AuthViewModel = viewModel(factory = factory)
                SideEffect { auth = vm }
                MaterialTheme {
                    if (registration) RegisterScreen(vm.registerState, vm::changeRegistration, vm::register, {})
                    else LoginScreen(vm.loginState, { vm.changeLogin(alias = it) },
                        { vm.changeLogin(password = it) }, { vm.changeLogin(role = it) }, vm::login, {}, {})
                }
            }
        }
        compose.waitForIdle()
    }

    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasText(label))

    @Test fun registrationKeepsUnrelatedErrorsAndProvidesSpecificAccessibilityText() {
        render(true)
        registerButton().performScrollTo().performClick()
        field("Nombre").performScrollTo().performTextInput(valid.name)
        field("Nombre").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error).not())
        field("Correo electrónico").assert(SemanticsMatcher.expectValue(SemanticsProperties.Error,
            "Correo electrónico: Ingresa un correo de prueba válido"))
        field("Alias").performScrollTo().performTextInput(valid.alias)
        compose.runOnIdle {
            assertFalse(auth.registerState.errors.containsKey("alias"))
            assertTrue(auth.registerState.errors.containsKey("email"))
            assertTrue(auth.registerState.errors.containsKey("password"))
        }
    }

    @Test fun loginKeepsPasswordErrorWhenAliasOrRoleChanges() {
        render(false)
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        field("Alias").performScrollTo().performTextInput(valid.alias)
        field("Contraseña").assert(SemanticsMatcher.expectValue(SemanticsProperties.Error,
            "Contraseña: Ingresa tu contraseña"))
        compose.onNodeWithText("Administrador").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(UserRole.ADMIN, auth.loginState.role)
            assertTrue(auth.loginState.errors.containsKey("password"))
        }
        field("Contraseña").performScrollTo().performTextInput(valid.password)
        field("Contraseña").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error).not())
    }

    @Test fun passwordErrorKeepsTheVisibilityButtonWorking() {
        render(false)
        compose.runOnIdle { auth.changeLogin(alias = valid.alias); auth.login() }
        field("Contraseña").performScrollTo()
        field("Contraseña").assert(SemanticsMatcher.expectValue(SemanticsProperties.Password, Unit))
        compose.onNodeWithContentDescription("Mostrar contraseña").performClick()
        compose.onNodeWithContentDescription("Ocultar contraseña").assertExists()
        field("Contraseña").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password).not())
        field("Contraseña").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.onNodeWithContentDescription("Ocultar contraseña").performClick()
        field("Contraseña").assert(SemanticsMatcher.expectValue(SemanticsProperties.Password, Unit))
    }

    @Test fun duplicateAliasKeepsFormValuesAndItsErrorWhenNameChanges() {
        registerAction = { throw AliasAlreadyUsedException() }
        render(true)
        compose.runOnIdle { auth.changeRegistration(valid); auth.register() }
        field("Alias").assert(SemanticsMatcher.expectValue(SemanticsProperties.Error,
            "Alias: Este alias ya está registrado"))
        field("Nombre").performScrollTo().performTextReplacement("Otro Nombre")
        field("Alias").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.runOnIdle {
            assertEquals(valid.email, auth.registerState.form.email)
            assertEquals(valid.password, auth.registerState.form.password)
        }
    }

    @Test fun registrationButtonAndFieldsAreDisabledDuringOnePendingWrite() {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        registerAction = { calls++; gate.await() }
        render(true)
        compose.runOnIdle { auth.changeRegistration(valid) }
        registerButton().performScrollTo().performClick()
        compose.onNodeWithText("Procesando…").assertIsNotEnabled()
        // Disabled text fields deliberately do not expose SetText.
        compose.onNodeWithText("Alias").assertIsNotEnabled()
        compose.runOnIdle { auth.register(); assertEquals(1, calls); gate.complete(Unit) }
        compose.waitForIdle()
        compose.onNode(hasClickAction() and hasText("Crear cuenta")).assertIsNotEnabled()
    }

    @Test fun loginButtonAndVisibilityAreDisabledDuringVerification() {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        loginAction = { _, _, _ -> calls++; gate.await(); null }
        render(false)
        compose.runOnIdle { auth.changeLogin(valid.alias, valid.password) }
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        compose.onNodeWithText("Procesando…").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Mostrar contraseña").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { auth.login(); assertEquals(1, calls); gate.complete(Unit) }
        compose.waitForIdle()
        compose.onNodeWithText("Revisa el alias, la contraseña y el tipo de usuario.").performScrollTo().assertIsDisplayed()
    }

    @Test fun activityRecreationRetainsFormValuesAndErrorsWithoutSavingPasswordToBundle() {
        render(true)
        compose.runOnIdle {
            auth.changeRegistration(valid.copy(email = "mal", confirmation = "Distinta123"))
            auth.register()
            auth.changeLogin(valid.alias, valid.password, UserRole.ADMIN)
        }
        val original = auth
        compose.activityRule.scenario.onActivity { activity ->
            val state = Bundle()
            InstrumentationRegistry.getInstrumentation().callActivityOnSaveInstanceState(activity, state)
            val parcel = Parcel.obtain()
            val bytes = try { parcel.writeBundle(state); parcel.marshall() } finally { parcel.recycle() }
            for (secret in listOf(valid.password, "Distinta123")) {
                for (charset in listOf(Charsets.UTF_8, Charsets.UTF_16LE)) {
                    val needle = secret.toByteArray(charset)
                    val found = (0..(bytes.size - needle.size).coerceAtLeast(-1)).any { offset ->
                        needle.indices.all { bytes[offset + it] == needle[it] }
                    }
                    assertFalse("A password must not enter saved instance state", found)
                }
            }
        }
        compose.activityRule.scenario.recreate()
        render(true)
        compose.runOnIdle {
            assertSame(original, auth)
            assertEquals(valid.password, auth.registerState.form.password)
            assertEquals(setOf("email", "confirmation"), auth.registerState.errors.keys)
            assertEquals(valid.alias, auth.loginState.alias)
            assertEquals(valid.password, auth.loginState.password)
            assertEquals(UserRole.ADMIN, auth.loginState.role)
        }
        field("Correo electrónico").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    // Registration has a title and button with the same text; select the actionable node.
    private fun registerButton(): SemanticsNodeInteraction =
        compose.onNode(hasClickAction() and hasText("Crear cuenta"))
}
