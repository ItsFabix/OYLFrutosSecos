package com.example.oylfrutossecos

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.ui.navigation.StoreNavigation
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Real NavHost and repositories; all SQLite access is redirected to a test-owned cache file.
 * Only navigation and reads: never posts orders, changes stock, or opens the user's database.
 */
class HomeNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var database: OylDatabase
    private var previousDatabase: OylDatabase? = null
    private lateinit var client: User
    private val singleton = OylDatabase::class.java.getDeclaredField("instance").apply { isAccessible = true }

    @Before fun isolatedDatabase() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(base.cacheDir, "home-navigation-tests/${UUID.randomUUID()}/oyl_auth.db")
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
        val form = Registration("Cliente de Prueba", "navtest", "navtest@example.cl", "912345678",
            "Calle de Prueba 123", "Cliente123", "Cliente123")
        val id = database.registerClient(form)
        client = User(id, form.name, form.alias, form.email, form.phone, form.address, UserRole.CLIENT)
        previousDatabase = singleton.get(null) as OylDatabase?
        singleton.set(null, database)
    }

    @After fun restoreSingleton() {
        if (::database.isInitialized) {
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent {}
                activity.viewModelStore.clear()
            }
            compose.waitForIdle()
            singleton.set(null, previousDatabase)
            database.close()
        }
    }

    private fun render(admin: Boolean) {
        val user = if (admin) client.copy(id = 1, name = "Administrador", alias = "admin", role = UserRole.ADMIN) else client
        compose.setContent { MaterialTheme { StoreNavigation(user, false, null, {}) } }
        compose.waitUntil(30000) { compose.onAllNodesWithText("Hola, ${user.name}").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun home() {
        compose.onNodeWithText("Inicio").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Resumen administrativo")
            .fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Descubre nuestros productos")
            .fetchSemanticsNodes().isNotEmpty() }
    }

    private fun open(label: String, expected: String) {
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test fun clientDashboardNavigatesToExistingDestinationsAndReturnsHome() {
        render(false)
        open("Ver carrito", "Mi carrito")
        home()
        open("Mis pedidos", "Mis pedidos")
        home()
        open("Explorar catálogo", "Encuentra tus favoritos")
        home()
        // Match the unread badge without relying on its count.
        compose.onNode(hasClickAction() and hasText("Notificaciones", substring = true))
            .performScrollTo().performClick()
        compose.onNodeWithText("Avisos guardados en este dispositivo; no se reciben notificaciones del servidor.")
            .assertIsDisplayed()
        home()
        compose.onNodeWithText("Ver carrito").assertIsDisplayed()
    }

    @Test fun adminDashboardNavigatesToExistingDestinationsAndReturnsHome() {
        render(true)
        open("Administrar productos", "Gestión de productos")
        home()
        open("Inventario", "Gestión de inventario")
        home()
        open("Pedidos", "Gestionar pedidos")
        home()
        compose.onNodeWithText("Resumen de inventario").assertIsDisplayed()
    }
}
