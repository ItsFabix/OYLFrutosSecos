package com.example.oylfrutossecos

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.oylfrutossecos.ui.theme.OYLFrutosSecosTheme
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
        compose.setContent { OYLFrutosSecosTheme { StoreNavigation(user, false, null, {}) } }
        compose.waitUntil(30000) { compose.onAllNodesWithText("Hola, ${user.name}").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun home() {
        compose.onNodeWithText("Inicio").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("home-list").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home-list").performScrollToIndex(0)
        compose.waitUntil(10000) { compose.onAllNodesWithText("Resumen administrativo")
            .fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Descubre nuestros productos")
            .fetchSemanticsNodes().isNotEmpty() }
    }

    private fun open(label: String, expected: String) {
        compose.onNodeWithTag("home-list").performScrollToNode(hasText(label))
        compose.onNodeWithText(label).performClick()
        compose.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test fun clientDashboardNavigatesToExistingDestinationsAndReturnsHome() {
        render(false)
        open("Ver carrito", "Mi carrito (0)")
        home()
        open("Mis pedidos", "Mis pedidos")
        home()
        open("Explorar catálogo", "Buscar")
        home()
        // Match the unread badge without relying on its count.
        compose.onNodeWithTag("home-list").performScrollToNode(hasText("Notificaciones", substring = true))
        compose.onNode(hasClickAction() and hasText("Notificaciones", substring = true))
            .performClick()
        compose.onNodeWithText("Avisos de tus pedidos y productos en este dispositivo.")
            .assertIsDisplayed()
        home()
        compose.onNodeWithText("Ver carrito").assertIsDisplayed()
    }

    @Test fun adminDashboardNavigatesToExistingDestinationsAndReturnsHome() {
        render(true)
        open("Gestionar productos", "Gestión de productos")
        home()
        open("Inventario", "Gestión de inventario")
        home()
        open("Pedidos", "Gestionar pedidos")
        home()
        compose.onNodeWithText("Resumen general").assertIsDisplayed()
    }

    private fun snapshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "sqlite-design-qa")
        check(directory.exists() || directory.mkdirs())
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun localCheckoutAndAdminStatusesThroughRealNavigationAtSmallWidth() {
        val selected = database.readProducts().first { it.stock >= 2 }
        database.setCartQuantity(client.id, selected.id, 2)
        val admin = database.authenticate("admin", "Admin1234", UserRole.ADMIN)!!
        val current = mutableStateOf(client)
        compose.setContent {
            OYLFrutosSecosTheme {
                Box(Modifier.width(320.dp)) {
                    key(current.value.id) { StoreNavigation(current.value, false, null, {}) }
                }
            }
        }
        compose.waitUntil(30000) {
            compose.onAllNodesWithText("Ver carrito").fetchSemanticsNodes().isNotEmpty()
        }
        snapshot("cliente-inicio")
        compose.onNodeWithText("Ver carrito").performScrollTo().performClick()
        compose.onNodeWithText("Continuar al pedido").performScrollTo().assertIsEnabled()
        snapshot("cliente-carrito")
        compose.onNodeWithText("Continuar al pedido").performClick()
        compose.waitUntil(30000) {
            compose.onAllNodesWithText("Resumen").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("confirmation-list").performScrollToNode(hasText("Registrar pedido"))
        compose.onNodeWithText("Registrar pedido").assertIsEnabled()
        snapshot("cliente-confirmacion")
        compose.onNodeWithText("Registrar pedido").performClick()
        compose.waitUntil(30000) {
            compose.onAllNodesWithText("Pedido registrado").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Ver mis pedidos").performScrollTo().performClick()
        compose.waitUntil(30000) {
            compose.onAllNodesWithText("Estado: ${OrderStatus.PENDING.displayName}").fetchSemanticsNodes().isNotEmpty()
        }
        org.junit.Assert.assertTrue(database.readCart(client.id).isEmpty())
        org.junit.Assert.assertEquals(1, database.readOrders(client.id).size)
        org.junit.Assert.assertEquals(selected.stock,
            database.readProducts().first { it.id == selected.id }.stock)

        compose.runOnIdle { current.value = admin }
        compose.waitUntil(30000) {
            compose.onAllNodesWithText("Resumen administrativo").fetchSemanticsNodes().isNotEmpty()
        }
        snapshot("admin-inicio")
        open("Pedidos", "Gestionar pedidos")
        for (status in listOf("Confirmado", "Preparado", "Entregado")) {
            compose.onNodeWithTag("orders-list").performScrollToNode(hasText("Marcar como $status"))
            compose.onNodeWithText("Marcar como $status").performClick()
            compose.waitUntil(30000) {
                compose.onAllNodesWithText("Estado: $status").fetchSemanticsNodes().isNotEmpty()
            }
        }
        snapshot("admin-pedido-entregado")
        compose.runOnIdle { current.value = client }
        compose.waitUntil(30000) {
            compose.onAllNodesWithText("Ver carrito").fetchSemanticsNodes().isNotEmpty()
        }
        open("Mis pedidos", "Mis pedidos")
        compose.waitUntil(30000) {
            compose.onAllNodesWithText("Estado: Entregado").fetchSemanticsNodes().isNotEmpty()
        }
        snapshot("cliente-pedido-entregado")
        compose.onNodeWithText("Cuenta").performClick()
        compose.onNodeWithText("Conexión con el servidor").assertDoesNotExist()
    }

    @Test fun captureProfessorReferenceAt320360And393dpWithBothRoles() {
        val chosen = database.readProducts().first { it.stock >= 2 }
        database.setCartQuantity(client.id, chosen.id, 1)
        database.placeDemoOrder(client, DeliveryMethod.PICKUP, database.readCart(client.id))
        database.setCartQuantity(client.id, chosen.id, 2)
        val admin = database.authenticate("admin", "Admin1234", UserRole.ADMIN)!!
        val selection = mutableStateOf(320 to client)
        compose.setContent {
            OYLFrutosSecosTheme {
                Box(Modifier.size(selection.value.first.dp, (selection.value.first * 2).dp).testTag("reference-root")) {
                    key(selection.value.first, selection.value.second.id) {
                        StoreNavigation(selection.value.second, false, null, {})
                    }
                }
            }
        }
        fun capture(name: String) {
            compose.mainClock.advanceTimeBy(600)
            compose.waitForIdle()
            val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "professor-reference-qa")
            check(directory.exists() || directory.mkdirs())
            val bitmap = compose.onNodeWithTag("reference-root").captureToImage().asAndroidBitmap()
            File(directory, name + ".png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        for (width in listOf(320, 360, 393)) for (user in listOf(client, admin)) {
            compose.runOnIdle { selection.value = width to user }
            val role = if (user.role == UserRole.ADMIN) "admin" else "cliente"
            compose.waitUntil(30000) {
                if (role == "admin") compose.onAllNodes(hasText("Productos") and hasText("8")).fetchSemanticsNodes().isNotEmpty()
                else compose.onAllNodesWithText("2 bolsas · Total: ${com.example.oylfrutossecos.ui.components.priceInPesos(chosen.priceClp * 2)}")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("home-list").performScrollToIndex(0)
            capture("$width-$role-inicio")
            compose.onNodeWithText("Catálogo").performClick()
            compose.waitUntil(30000) { compose.onAllNodesWithText("Almendras naturales").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("catalog-list").performScrollToIndex(0)
            val first = compose.onNodeWithText("Almendras naturales").fetchSemanticsNode().boundsInRoot
            val second = compose.onNodeWithText("Nueces mariposa").fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue("El catálogo debe tener dos columnas", first.left < second.left && kotlin.math.abs(first.top - second.top) < 2f)
            capture("$width-$role-catalogo")
            compose.onNodeWithText("QR").performClick()
            compose.onNodeWithText("Escanear con cámara").assertIsDisplayed().assertIsEnabled()
            capture("$width-$role-qr")
            if (user.role == UserRole.ADMIN) {
                compose.onNodeWithText("Gestión").performClick()
                compose.waitUntil(30000) { compose.onAllNodesWithContentDescription("Editar Almendras naturales").fetchSemanticsNodes().isNotEmpty() }
                capture("$width-$role-productos")
            }
            compose.onNodeWithText("Cuenta").performClick()
            compose.onNodeWithText("Cerrar sesión").assertIsDisplayed().assertIsEnabled()
            capture("$width-$role-perfil")
        }
    }
}
