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
    private lateinit var api: NavigationTestApi
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
        api = NavigationTestApi(database)
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

    private fun render(admin: Boolean, onLogout: () -> Unit = {}) {
        val user = if (admin) client.copy(id = 1, name = "Administrador", alias = "admin", role = UserRole.ADMIN) else client
        compose.setContent { OYLFrutosSecosTheme { StoreNavigation(user, false, null, onLogout, api) } }
        compose.waitUntil(30000) { compose.onAllNodesWithText("Hola, ${user.name}").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun leavingAndReopeningPickupMapStartsInOverviewAgain() {
        render(false)
        open("Puntos de retiro", "Vista general · 2 locales")
        compose.onNodeWithText("Ver todos").assertIsSelected()
        compose.onNodeWithText("Ñuñoa").performClick().assertIsSelected()
        compose.onNodeWithText("Referencia: Plaza Ñuñoa, Santiago").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithTag("home-list").assertExists()
        open("Puntos de retiro", "Vista general · 2 locales")
        compose.onNodeWithText("Ver todos").assertIsSelected()
        compose.onNodeWithText("Ñuñoa").assertIsNotSelected()
        compose.waitUntil(10000) {
            var fitted = false
            compose.runOnIdle {
                val map = compose.activity.findViewById<android.view.View>(android.R.id.content)
                    .findViewWithTag<org.osmdroid.views.MapView>("pickup-map")
                fitted = map.overlays.filterIsInstance<org.osmdroid.views.overlay.Marker>().size == 2 &&
                    com.example.oylfrutossecos.data.repository.DemoPickupPoints.all.all {
                        map.boundingBox.contains(it.latitude, it.longitude)
                    }
            }
            fitted
        }
    }

    @Test fun profileEditsRefreshAccountAndPersistAfterLogoutAndLoginAt320dp() {
        database.saveSession(client.id)
        lateinit var auth: com.example.oylfrutossecos.ui.viewmodel.AuthViewModel
        compose.activityRule.scenario.onActivity {
            auth = androidx.lifecycle.ViewModelProvider(it,
                com.example.oylfrutossecos.ui.viewmodel.AuthViewModel.Factory(
                    com.example.oylfrutossecos.data.repository.AuthRepository(database)))[com.example.oylfrutossecos.ui.viewmodel.AuthViewModel::class.java]
        }
        compose.setContent { OYLFrutosSecosTheme {
            Box(Modifier.size(320.dp, 640.dp)) {
                val user = auth.user
                if (user != null) key(user.id) {
                    StoreNavigation(user, auth.loggingOut, auth.sessionError, auth::logout, api, auth::acceptUpdatedProfile)
                } else androidx.compose.material3.Text("Sesión cerrada")
            }
        } }
        compose.waitUntil(30000) { compose.onAllNodesWithText("Cuenta").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Cuenta").performClick()
        compose.onNodeWithText("Editar perfil").assertIsDisplayed().performClick()
        compose.onNodeWithText("Nombre").performTextReplacement("X")
        compose.onNodeWithText("Guardar cambios").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa un nombre de 2 a 80 caracteres.").assertExists()
        compose.onNodeWithText("Nombre").performScrollTo().performTextReplacement("Cliente Editado")
        compose.onNodeWithText("Teléfono").performScrollTo().performTextReplacement("987654321")
        compose.onNodeWithText("Dirección").performScrollTo().performTextReplacement("Calle Nueva 456")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Guardar cambios").performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Perfil actualizado").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Cliente Editado").assertIsDisplayed()
        compose.onNodeWithTag("account-list").performScrollToNode(hasText("Datos de la cuenta"))
        compose.onNodeWithText("Datos de la cuenta").performClick()
        compose.onNodeWithTag("account-list").performScrollToNode(hasText("Calle Nueva 456"))
        compose.onNodeWithText("Calle Nueva 456").assertIsDisplayed()
        compose.onNodeWithText("987654321").assertIsDisplayed()
        compose.onNodeWithTag("account-list").performScrollToNode(hasText("Cerrar sesión"))
        compose.onNodeWithText("Cerrar sesión").performClick()
        compose.waitUntil(30000) { auth.user == null && !auth.loggingOut }
        compose.runOnIdle {
            auth.changeLogin(client.alias, "Cliente123", UserRole.CLIENT)
            auth.login()
        }
        compose.waitUntil(30000) { auth.user != null }
        compose.onNodeWithText("Cuenta").performClick()
        compose.onNodeWithText("Cliente Editado").assertIsDisplayed()
        compose.onNodeWithText("Editar perfil").performClick()
        compose.onNodeWithText("987654321").assertExists()
        compose.onNodeWithText("Calle Nueva 456").assertExists()
        org.junit.Assert.assertEquals(client.id, auth.user!!.id)
        org.junit.Assert.assertEquals(client.alias, auth.user!!.alias)
        org.junit.Assert.assertEquals(UserRole.CLIENT, auth.user!!.role)
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
        compose.onNodeWithText("Mantente al día con tus pedidos y novedades.")
            .assertIsDisplayed()
        home()
        compose.onNodeWithText("Ver carrito").assertIsDisplayed()
        compose.onNodeWithText("Configuración administrativa").assertDoesNotExist()
        compose.onNodeWithText("Cuenta").performClick()
        compose.onNodeWithText("Configuración administrativa").assertDoesNotExist()
        compose.onNodeWithText("Configurar acceso").assertDoesNotExist()
        compose.onNodeWithText("Clave administrativa").assertDoesNotExist()
    }

    @Test fun adminDashboardNavigatesToExistingDestinationsAndReturnsHome() {
        render(true)
        open("Gestionar productos", "Gestión de productos")
        home()
        open("Inventario", "Gestión de inventario")
        compose.onNodeWithText("Acceso administrativo requerido").assertIsDisplayed()
        home()
        open("Pedidos", "Gestionar pedidos")
        compose.onNodeWithText("Acceso administrativo requerido").assertIsDisplayed()
        home()
        compose.onNodeWithText("Resumen general").assertIsDisplayed()
    }

    private fun configureAccess(key: String) {
        compose.onNodeWithText("Clave administrativa").performScrollTo().performTextInput(key)
        compose.onNodeWithText("Conectar administración").performScrollTo().performClick()
    }

    private fun openAdminSettings() {
        compose.onNodeWithText("Cuenta").performClick()
        compose.onNodeWithTag("account-list").performScrollToNode(hasText("Configuración administrativa"))
        compose.onNodeWithText("Configuración administrativa").performClick()
        compose.onNodeWithTag("admin-settings").assertExists()
    }

    private fun session() = androidx.lifecycle.ViewModelProvider(compose.activity)
        .get("admin-session-1", com.example.oylfrutossecos.ui.viewmodel.AdminSessionViewModel::class.java)

    @Test fun missingAccessCanBeConfiguredFromInventoryAndLogoutClearsItBeforeCallback() {
        var loggedOut = false
        render(true) {
            org.junit.Assert.assertFalse(session().configured)
            org.junit.Assert.assertEquals("", session().key())
            loggedOut = true
        }
        open("Inventario", "Gestión de inventario")
        compose.onNodeWithText("Acceso administrativo requerido").assertIsDisplayed()
        compose.onNodeWithText("Configurar acceso").performClick()
        configureAccess("navigation-test-admin-key")
        compose.waitUntil(10000) { session().configured }
        compose.onNodeWithText("Clave administrativa").assertDoesNotExist()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Registrar entrada / salida de Almendras naturales")
            .fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Acceso administrativo requerido").assertDoesNotExist()
        compose.onNodeWithText("Cuenta").performClick()
        compose.onNodeWithTag("account-list").performScrollToNode(hasText("Cerrar sesión"))
        compose.onNodeWithText("Cerrar sesión").performClick()
        compose.runOnIdle {
            org.junit.Assert.assertTrue(loggedOut)
            org.junit.Assert.assertEquals("", session().key())
        }
    }

    @Test fun server403RequiresAccessAgainAndCanReplaceKeyWithoutClosingLocalSession() {
        render(true)
        openAdminSettings()
        configureAccess("navigation-test-admin-key")
        compose.waitUntil(10000) { session().configured }
        home()
        open("Pedidos", "Gestionar pedidos")
        compose.waitUntil(10000) { compose.onAllNodesWithText("Actualizar pedidos").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { api.acceptedAdminKey = "rotated-test-administrative-key" }
        compose.onNodeWithText("Actualizar pedidos").assertIsEnabled().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Acceso administrativo requerido").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { org.junit.Assert.assertEquals("", session().key()) }
        compose.onNodeWithText("Configurar acceso").performClick()
        configureAccess("rotated-test-administrative-key")
        compose.waitUntil(10000) { session().configured }
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Acceso administrativo requerido").assertDoesNotExist()
        compose.onNodeWithText("Actualizar pedidos").assertIsDisplayed()
    }

    @Test fun adminConfiguresOnlyFromAccountAndCanCorrectReplaceAndDisconnectWithoutLogout() {
        render(true)
        compose.onNodeWithText("Configurar acceso al servidor").assertDoesNotExist()
        compose.onNodeWithText("Configurar acceso").assertDoesNotExist()
        compose.onNodeWithText("Configuración administrativa").assertDoesNotExist()
        compose.onNodeWithText("Inventario no disponible").assertDoesNotExist()
        compose.onNodeWithText("Historial no disponible").assertDoesNotExist()
        openAdminSettings()
        configureAccess("incorrect-test-administrative-key")
        compose.waitUntil(10000) { compose.onAllNodesWithText("La clave administrativa no es válida", substring = true)
            .fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Acceso administrativo conectado").assertDoesNotExist()
        configureAccess("navigation-test-admin-key")
        compose.waitUntil(10000) { compose.onAllNodesWithText("Acceso administrativo conectado").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(10000) {
            val provider = androidx.lifecycle.ViewModelProvider(compose.activity)
            val orders = provider["orders-1", com.example.oylfrutossecos.ui.viewmodel.OrderViewModel::class.java].state.value
            val inventory = provider["inventory-1", com.example.oylfrutossecos.ui.viewmodel.InventoryViewModel::class.java].state.value
            orders.remoteFresh && !orders.loading && orders.error == null && !inventory.loading && inventory.error == null && inventory.products.size == 8
        }
        compose.onNodeWithText("Inventario no disponible").assertDoesNotExist()
        compose.onNodeWithText("Historial no disponible").assertDoesNotExist()
        compose.onNodeWithText("Clave administrativa").assertDoesNotExist()
        compose.onNodeWithText("Cambiar clave").performScrollTo().performClick()
        configureAccess("incorrect-replacement-test-key")
        compose.waitUntil(10000) { !session().configured && session().error != null }
        compose.runOnIdle { org.junit.Assert.assertEquals("", session().key()) }
        configureAccess("navigation-test-admin-key")
        compose.waitUntil(10000) { session().configured }
        compose.onNodeWithText("Desconectar acceso").performScrollTo().performClick()
        compose.onNodeWithText("Acceso administrativo conectado").assertDoesNotExist()
        compose.onNodeWithText("Clave administrativa").assertExists()
        compose.runOnIdle { org.junit.Assert.assertEquals("", session().key()) }
    }

    @Test fun authorizedAdminDoesNotNeedKeyAgainAfterNavigationOrActivityRecreation() {
        database.saveSession(1)
        render(true)
        openAdminSettings()
        configureAccess("navigation-test-admin-key")
        compose.waitUntil(10000) { compose.onAllNodesWithText("Acceso administrativo conectado").fetchSemanticsNodes().isNotEmpty() }
        home()
        open("Inventario", "Gestión de inventario")
        compose.onNodeWithText("Acceso administrativo requerido").assertDoesNotExist()
        compose.onNodeWithText("Configurar acceso").assertDoesNotExist()
        home()
        open("Pedidos", "Gestionar pedidos")
        compose.onNodeWithText("Acceso administrativo requerido").assertDoesNotExist()
        compose.onNodeWithText("Clave administrativa").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity { activity ->
            val restored = database.readSession()!!
            org.junit.Assert.assertEquals(UserRole.ADMIN, restored.role)
            activity.setContent { OYLFrutosSecosTheme { StoreNavigation(restored, false, null, {}, api) } }
        }
        compose.waitForIdle()
        compose.runOnIdle { org.junit.Assert.assertTrue(session().configured) }
        compose.onNodeWithText("Acceso administrativo requerido").assertDoesNotExist()
        openAdminSettings()
        compose.onNodeWithText("Acceso administrativo conectado").assertIsDisplayed()
        compose.onNodeWithText("Clave administrativa").assertDoesNotExist()
    }

    private fun snapshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "sqlite-design-qa")
        check(directory.exists() || directory.mkdirs())
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun restCheckoutAndAdminStatusesThroughRealNavigationAtSmallWidth() {
        val selected = database.readProducts().first { it.stock >= 2 }
        database.setCartQuantity(client.id, selected.id, 2)
        val admin = database.authenticate("admin", "Admin1234", UserRole.ADMIN)!!
        val current = mutableStateOf(client)
        compose.setContent {
            OYLFrutosSecosTheme {
                Box(Modifier.width(320.dp)) {
                    key(current.value.id) { StoreNavigation(current.value, false, null, {}, api) }
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
        org.junit.Assert.assertEquals(selected.stock - 2,
            database.readProducts().first { it.id == selected.id }.stock)

        compose.runOnIdle { current.value = admin }
        compose.waitUntil(30000) {
            compose.onAllNodesWithText("Resumen administrativo").fetchSemanticsNodes().isNotEmpty()
        }
        snapshot("admin-inicio")
        open("Pedidos", "Gestionar pedidos")
        compose.onNodeWithText("Acceso administrativo requerido").assertIsDisplayed()
        compose.onNodeWithText("Configurar acceso").performClick()
        configureAccess("navigation-test-admin-key")
        compose.waitUntil(10000) { session().configured }
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Marcar como Confirmado")
            .fetchSemanticsNodes().isNotEmpty() }
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
                        StoreNavigation(selection.value.second, false, null, {}, api)
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
