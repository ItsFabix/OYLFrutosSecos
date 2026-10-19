package com.example.oylfrutossecos.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.data.model.DeliveryMethod
import com.example.oylfrutossecos.data.model.UserRole
import com.example.oylfrutossecos.data.repository.CartRepository
import com.example.oylfrutossecos.data.repository.ProductRepository
import com.example.oylfrutossecos.data.repository.OrderRepository
import com.example.oylfrutossecos.ui.screens.*
import com.example.oylfrutossecos.ui.viewmodel.CatalogViewModel
import com.example.oylfrutossecos.ui.viewmodel.CartViewModel
import com.example.oylfrutossecos.ui.viewmodel.OrderViewModel

@Composable
fun StoreNavigation(user: User, busy: Boolean, error: String?, onLogout: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val database = remember(context) { OylDatabase.getInstance(context) }
    val catalogFactory = remember(database) { CatalogViewModel.Factory(ProductRepository(database)) }
    val cartFactory = remember(database, user.id) { CartViewModel.Factory(CartRepository(database), user.id) }
    val orderFactory = remember(database, user.id) { OrderViewModel.Factory(OrderRepository(database), user) }
    val catalog: CatalogViewModel = viewModel(key = "catalog-${user.id}", factory = catalogFactory)
    val cart: CartViewModel = viewModel(key = "cart-${user.id}", factory = cartFactory)
    val orders: OrderViewModel = viewModel(key = "orders-${user.id}", factory = orderFactory)
    val state by catalog.state.collectAsStateWithLifecycle()
    val cartState by cart.state.collectAsStateWithLifecycle()
    val orderState by orders.state.collectAsStateWithLifecycle()
    val admin = user.role == UserRole.ADMIN
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route

    fun openTab(route: String) {
        nav.navigate(route) {
            popUpTo("store_home")
            launchSingleTop = true
        }
    }
    fun openProduct(id: Long) {
        cart.clearMessage()
        nav.navigate("product/$id") { launchSingleTop = true }
    }

    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(selected = current == "store_home", enabled = !busy,
                onClick = { openTab("store_home") }, icon = { Icon(Icons.Default.Home, null) },
                label = { Text("Inicio") })
            NavigationBarItem(selected = current == "catalog" || current == "product/{id}", enabled = !busy,
                onClick = { openTab("catalog") }, icon = { Icon(Icons.Default.ShoppingBag, null) },
                label = { Text("Catálogo") })
            if (admin) {
                NavigationBarItem(selected = current == "manage" || current == "editor/{id}",
                    enabled = !busy, onClick = { openTab("manage") },
                    icon = { Icon(Icons.Default.Inventory2, null) }, label = { Text("Productos") })
            } else {
                NavigationBarItem(selected = current == "cart", enabled = !busy,
                    onClick = { cart.reload(); openTab("cart") },
                    icon = { Icon(Icons.Default.ShoppingCart, null) }, label = { Text("Carrito") })
            }
            NavigationBarItem(selected = current == "profile" || current == "orders", enabled = !busy,
                onClick = { openTab("profile") }, icon = { Icon(Icons.Default.Person, null) },
                label = { Text("Cuenta") })
        }
    }) { padding ->
        NavHost(navController = nav, startDestination = "store_home",
            modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("store_home") {
                HomeScreen(user, state, { openTab("catalog") }, ::openProduct, catalog::load)
            }
            composable("catalog") {
                CatalogScreen(state, catalog::search, catalog::selectCategory,
                    catalog::clearFilters, catalog::load, ::openProduct)
            }
            composable("product/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { route ->
                val id = route.arguments?.getLong("id")
                ProductDetailScreen(state.products.firstOrNull { it.id == id }, state.loading,
                    state.error, catalog::load, { nav.popBackStack() },
                    isAdmin = admin, onEdit = { if (admin && id != null) nav.navigate("editor/$id") },
                    cartBusy = cartState.busy || cartState.loading,
                    cartNotice = cartState.notice, cartError = cartState.error,
                    onAdd = { quantity -> if (!admin && id != null) cart.add(id, quantity) },
                    onCart = { if (!admin) { cart.reload(); openTab("cart") } })
            }
            if (admin) {
                composable("manage") {
                    ManageProductsScreen(state, { nav.navigate("editor/0") },
                        { nav.navigate("editor/$it") }, catalog::load)
                }
                composable("editor/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { route ->
                    val id = route.arguments?.getLong("id") ?: 0L
                    val product = state.products.firstOrNull { it.id == id }
                    if (id != 0L && product == null) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Producto no encontrado")
                            TextButton(onClick = { nav.popBackStack() }) { Text("Volver") }
                        }
                    } else {
                        EditProductScreen(product, id == 0L,
                            onBack = { nav.popBackStack() },
                            onSave = { draft, done ->
                                catalog.saveProduct(id.takeUnless { it == 0L }, draft, done)
                            })
                    }
                }
            } else {
                composable("cart") {
                    CartScreen(cartState, { id, quantity -> cart.change(id, quantity) },
                        cart::reload, { openTab("catalog") },
                        { cart.reload(); nav.navigate("order_confirmation") })
                }
            }
            if (!admin) {
                composable("order_confirmation") {
                    OrderConfirmationScreen(user, cartState, orderState,
                        onBack = { nav.popBackStack() },
                        onConfirm = { delivery: DeliveryMethod ->
                            orders.place(delivery, cartState.items) {
                                cart.reload()
                                nav.navigate("order_success") {
                                    popUpTo("cart") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        })
                }
                composable("order_success") {
                    OrderSuccessScreen(orderState.placedId,
                        onOrders = { orders.load(); openTab("orders") },
                        onHome = { openTab("store_home") })
                }
            }
            composable("orders") {
                OrdersScreen(orderState, admin,
                    onBack = { nav.popBackStack() },
                    onRetry = orders::load, onAdvance = orders::advance)
            }
            composable("profile") {
                AccountScreen(user, busy, error, onLogout,
                    onOrdersClick = { orders.load(); nav.navigate("orders") { launchSingleTop = true } })
            }
        }
    }
}
