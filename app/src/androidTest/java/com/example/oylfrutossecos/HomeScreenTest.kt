package com.example.oylfrutossecos

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.oylfrutossecos.data.local.PendingCheckout
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.ui.screens.HomeScreen
import com.example.oylfrutossecos.ui.viewmodel.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** In-memory snapshots only: no application database, camera or order requests. */
class HomeScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val client = User(42, "Cliente Demo", "demo", "demo@example.cl", "912345678", "Calle 123", UserRole.CLIENT)
    private val products = listOf(0, 1, 5, 6).mapIndexed { index, stock ->
        Product(index.toLong() + 1, "Producto ${index + 1}", "Semillas", "Descripción", 1500, 100, stock, "seed")
    }
    private var selected = ""

    private fun render(admin: Boolean = false, cart: CartState = CartState(loading = false),
                       inventory: InventoryState = InventoryState(products = products, loading = false),
                       orders: OrderUiState = OrderUiState(loading = false), enabled: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                HomeScreen(client.copy(role = if (admin) UserRole.ADMIN else UserRole.CLIENT),
                    CatalogState(products = products, loading = false), cart, orders, inventory,
                    onCatalog = { selected = "catalog" }, onProduct = { selected = "product/$it" },
                    onRetry = {}, onMap = { selected = "map" }, onNotifications = { selected = "notifications" },
                    unreadNotices = 2, onCart = { selected = "cart" }, onOrders = { selected = "orders" },
                    onProducts = { selected = "manage" }, onInventory = { selected = "inventory" },
                    onRetryInventory = { selected = "retry" }, navigationEnabled = enabled)
            }
        }
    }

    private fun click(label: String, destination: String) {
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(destination, selected) }
    }

    @Test fun clientShowsExistingCartAndAllShortcuts() {
        render(cart = CartState(listOf(CartItem(products[1], 2)), loading = false))
        compose.onNodeWithText("Hola, Cliente Demo").assertIsDisplayed()
        compose.onNodeWithText("2 bolsas · Total: CLP 3000").assertIsDisplayed()
        click("Ver carrito", "cart")
        click("Mis pedidos", "orders")
        click("Explorar catálogo", "catalog")
        click("Notificaciones (2 nuevas)", "notifications")
        click("Mapa y puntos de retiro (demo)", "map")
        compose.onNodeWithText("Administrar productos").assertDoesNotExist()
    }

    @Test fun featuredProductsUseAvailabilityAndOpenTheExistingProduct() {
        render()
        compose.onNodeWithText("Producto 2").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("product/2", selected) }
        compose.onNodeWithText("Producto 1").assertDoesNotExist()
    }

    @Test fun pendingCheckoutRemainsVisibleEvenWithAnEmptyCart() {
        render(cart = CartState(loading = false,
            pending = PendingCheckout("existing-id", "{}", DeliveryMethod.PICKUP, "1:2")))
        compose.onNodeWithText("Tienes una compra pendiente de recuperación. Ábrela desde el carrito.")
            .assertIsDisplayed()
        click("Ver carrito", "cart")
    }

    @Test fun adminCountsOnlyLocalSnapshotsAndUsesExistingLowStockThreshold() {
        val order = OrderSummary(1, client.id, client.name, 0, OrderStatus.PENDING,
            DeliveryMethod.PICKUP, client.phone, client.address, 1500, emptyList())
        render(admin = true, orders = OrderUiState(orders = listOf(order, order.copy(id = 2,
            status = OrderStatus.DELIVERED)), loading = false))
        compose.onNodeWithText("Datos locales de demostración").assertExists()
        compose.onNode(hasText("Productos") and hasText("4")).assertExists()
        compose.onNode(hasText("Agotados") and hasText("1")).assertExists()
        compose.onNode(hasText("Stock bajo (1–5)") and hasText("2")).assertExists()
        compose.onNode(hasText("Pedidos pendientes") and hasText("1")).assertExists()
        click("Administrar productos", "manage")
        click("Inventario", "inventory")
        click("Pedidos", "orders")
        compose.onNodeWithText("Ver carrito").assertDoesNotExist()
    }

    @Test fun failedSourcesDoNotPresentZeroAsARealCountAndAllowRetry() {
        render(admin = true, inventory = InventoryState(products = products, loading = false, error = "fallo"),
            orders = OrderUiState(loading = false, error = "fallo"))
        compose.onNode(hasText("Productos") and hasText("—")).assertExists()
        compose.onNode(hasText("Pedidos pendientes") and hasText("—")).assertExists()
        click("Reintentar inventario", "retry")
    }

    @Test fun navigationLockAlsoDisablesDashboardShortcuts() {
        render(enabled = false)
        compose.onNodeWithText("Ver carrito").assertIsNotEnabled()
        compose.onNodeWithText("Mis pedidos").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Explorar catálogo").performScrollTo().assertIsNotEnabled()
    }
}
