package com.example.oylfrutossecos

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.repository.*
import com.example.oylfrutossecos.ui.viewmodel.AuthViewModel
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Each test uses a new database in cache; never accesses the singleton or existing users. */
class AuthSqliteTest {
    private lateinit var database: OylDatabase
    private lateinit var repository: AuthRepository
    private val store = ViewModelStore()
    private val valid = Registration("Cliente Nuevo", "nuevo", "nuevo@example.cl", "912345678",
        "Calle de Prueba 123", "Cliente123", "Cliente123")

    @Before fun isolatedDatabase() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(base.cacheDir, "auth-tests/${UUID.randomUUID()}/oyl_auth.db")
        check(file.parentFile!!.mkdirs())
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String): File = file
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?) =
                SQLiteDatabase.openOrCreateDatabase(file, factory)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?,
                errorHandler: DatabaseErrorHandler?) = SQLiteDatabase.openDatabase(file.absolutePath, factory,
                    SQLiteDatabase.CREATE_IF_NECESSARY, errorHandler)
        }
        database = OylDatabase::class.java.getDeclaredConstructor(Context::class.java)
            .apply { isAccessible = true }.newInstance(context)
        repository = AuthRepository(database)
    }

    @After fun closeTestResources() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        database.close()
    }

    private fun vm(): AuthViewModel {
        lateinit var result: AuthViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result = ViewModelProvider(store, AuthViewModel.Factory(repository))[AuthViewModel::class.java]
        }
        return result
    }

    private suspend fun awaitState(condition: () -> Boolean) {
        withTimeout(30000) { while (!condition()) delay(20) }
    }

    @Test fun validRegistrationAndLoginPreservePreexistingUsersAndAdminRole() = runBlocking {
        val existing = valid.copy(name = "Cliente Anterior", alias = "anterior", email = "anterior@example.cl")
        repository.register(existing)
        val previous = repository.login(existing.alias, existing.password, UserRole.CLIENT)!!
        val hashBefore = database.readableDatabase.rawQuery("SELECT password_hash FROM users WHERE id = ?",
            arrayOf(previous.id.toString())).use { it.moveToFirst(); it.getBlob(0) }
        repository.logout()
        val auth = vm()
        awaitState { !auth.restoring }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            auth.changeRegistration(valid)
            auth.register(); auth.register()
        }
        awaitState { auth.registerState.completed }
        assertEquals(3, database.readableDatabase.rawQuery("SELECT COUNT(*) FROM users", null)
            .use { it.moveToFirst(); it.getInt(0) })
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            auth.consumeRegistration()
            auth.changeLogin(valid.alias, valid.password, UserRole.CLIENT)
            auth.login(); auth.login()
        }
        awaitState { auth.user != null }
        assertEquals(valid.alias, auth.user!!.alias)
        assertEquals(UserRole.CLIENT, auth.user!!.role)
        assertEquals(auth.user, repository.currentUser())
        assertEquals(previous, repository.login(existing.alias, existing.password, UserRole.CLIENT))
        val hashAfter = database.readableDatabase.rawQuery("SELECT password_hash FROM users WHERE id = ?",
            arrayOf(previous.id.toString())).use { it.moveToFirst(); it.getBlob(0) }
        assertArrayEquals(hashBefore, hashAfter)
        val admin = repository.login("admin", "Admin1234", UserRole.ADMIN)
        assertNotNull(admin)
        assertEquals(UserRole.ADMIN, admin!!.role)
        assertNull(repository.login(existing.alias, existing.password, UserRole.ADMIN))
    }

    @Test fun duplicateAliasIsReportedByRealRepositoryWithoutReplacingTheExistingAccount() = runBlocking {
        repository.register(valid)
        val original = repository.login(valid.alias, valid.password, UserRole.CLIENT)!!
        repository.logout()
        val auth = vm()
        awaitState { !auth.restoring }
        val attempted = valid.copy(name = "Otro Nombre", alias = " NUEVO ", email = "otro@example.cl")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            auth.changeRegistration(attempted)
            auth.register()
        }
        awaitState { !auth.registerState.busy && auth.registerState.errors.containsKey("alias") }
        assertEquals(attempted, auth.registerState.form)
        assertEquals(setOf("alias"), auth.registerState.errors.keys)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            auth.changeRegistration(attempted.copy(email = "corregido@example.cl"))
        }
        assertTrue(auth.registerState.errors.containsKey("alias"))
        assertEquals(original, repository.login(valid.alias, valid.password, UserRole.CLIENT))
        assertEquals(2, database.readableDatabase.rawQuery("SELECT COUNT(*) FROM users", null)
            .use { it.moveToFirst(); it.getInt(0) })
    }
}
