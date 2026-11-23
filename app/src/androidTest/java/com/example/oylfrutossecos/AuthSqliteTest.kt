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
    private lateinit var isolatedContext: Context
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
        isolatedContext = context
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

    @Test fun profilePersistsAcrossDatabaseReopenAndLoginWithoutChangingCredentialsCartOrdersOrPendingRequest() = runBlocking {
        repository.register(valid)
        val original = repository.login(valid.alias, valid.password, UserRole.CLIENT)!!
        database.setCartQuantity(original.id, 1, 1)
        database.placeDemoOrder(original, DeliveryMethod.PICKUP, database.readCart(original.id))
        val ordersBefore = database.readOrders(original.id)
        database.setCartQuantity(original.id, 1, 2)
        val cartBefore = database.readCart(original.id)
        val cache = com.example.oylfrutossecos.data.local.RemoteCheckoutCache(database)
        val pending = cache.getOrCreate(original.id, UUID.randomUUID().toString(),
            "{\"contactAddress\":\"Dirección original\",\"contactPhone\":\"912345678\"}", DeliveryMethod.DELIVERY, cartBefore)
        fun credentials() = database.readableDatabase.rawQuery(
            "SELECT hex(password_salt), hex(password_hash) FROM users WHERE id = ?", arrayOf(original.id.toString())
        ).use { it.moveToFirst(); it.getString(0) to it.getString(1) }
        val credentialsBefore = credentials()
        val updated = repository.updateOwnProfile(original.id, ProfileDraft("  Nombre Editado  ", "+56 9 8765 4321", "  Calle Nueva 456  "))
        assertEquals(original.copy(name = "Nombre Editado", phone = "+56 9 8765 4321", address = "Calle Nueva 456"), updated)
        assertEquals(updated, repository.currentUser())
        assertEquals(credentialsBefore, credentials())
        database.close()
        database = OylDatabase::class.java.getDeclaredConstructor(Context::class.java)
            .apply { isAccessible = true }.newInstance(isolatedContext)
        repository = AuthRepository(database)
        assertEquals(updated, repository.currentUser())
        repository.logout()
        assertEquals(updated, repository.login(valid.alias, valid.password, UserRole.CLIENT))
        assertEquals(ordersBefore, database.readOrders(original.id))
        assertEquals(cartBefore, database.readCart(original.id))
        assertEquals(pending, com.example.oylfrutossecos.data.local.RemoteCheckoutCache(database).pending(original.id))
        assertEquals(8, database.readableDatabase.version)
    }

    @Test fun profileCannotUpdateAnotherUserOrUseAFormAfterSessionChangesOrLogout() = runBlocking {
        repository.register(valid)
        repository.register(valid.copy(alias = "segundo", email = "segundo@example.cl"))
        val first = repository.login(valid.alias, valid.password, UserRole.CLIENT)!!
        val second = database.authenticate("segundo", valid.password, UserRole.CLIENT)!!
        val draft = ProfileDraft("Nombre cambiado", "987654321", "Calle Nueva 456")
        suspend fun rejected(id: Long) {
            try { repository.updateOwnProfile(id, draft); fail("another user's profile updated") }
            catch (_: IllegalStateException) { }
        }
        rejected(second.id)
        repository.login("segundo", valid.password, UserRole.CLIENT)
        rejected(first.id)
        assertEquals(first, database.authenticate(first.alias, valid.password, UserRole.CLIENT))
        assertEquals(second, repository.currentUser())
        val updatedSecond = repository.updateOwnProfile(second.id, draft)
        assertEquals(second.id, updatedSecond.id)
        assertEquals(first, database.authenticate(first.alias, valid.password, UserRole.CLIENT))
        repository.logout()
        rejected(second.id)
        assertEquals(updatedSecond, database.authenticate(second.alias, valid.password, UserRole.CLIENT))
    }

    @Test fun invalidProfileIsRejectedByStorageWithoutPartialWrites() = runBlocking {
        repository.register(valid)
        val original = repository.login(valid.alias, valid.password, UserRole.CLIENT)!!
        listOf(ProfileDraft(), ProfileDraft("X", "123", "X"),
            ProfileDraft(original.name, "912345678", "a".repeat(161))).forEach { draft ->
            try { repository.updateOwnProfile(original.id, draft); fail("invalid profile saved") }
            catch (_: IllegalArgumentException) { }
            assertEquals(original, repository.currentUser())
        }
    }
}
