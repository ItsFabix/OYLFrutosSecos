package com.example.oylfrutossecos.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.oylfrutossecos.ui.notifications.DemoLocalNotifier
import com.example.oylfrutossecos.data.remote.OylRestClient
import com.example.oylfrutossecos.ui.viewmodel.RemoteCatalogViewModel
import com.example.oylfrutossecos.data.repository.NotificationRepository
import com.example.oylfrutossecos.ui.viewmodel.NotificationViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.data.model.DeliveryMethod
import com.example.oylfrutossecos.data.model.UserRole
import com.example.oylfrutossecos.data.model.NoticeKind
import com.example.oylfrutossecos.data.repository.CartRepository
import com.example.oylfrutossecos.data.repository.ProductRepository
import com.example.oylfrutossecos.data.repository.OrderRepository
import com.example.oylfrutossecos.data.repository.InventoryRepository
import com.example.oylfrutossecos.ui.screens.*
import com.example.oylfrutossecos.ui.viewmodel.CatalogViewModel
import com.example.oylfrutossecos.ui.viewmodel.CartViewModel
import com.example.oylfrutossecos.ui.viewmodel.OrderViewModel
import com.example.oylfrutossecos.ui.viewmodel.InventoryViewModel
import com.example.oylfrutossecos.ui.viewmodel.ProductFormViewModel
import com.example.oylfrutossecos.ui.viewmodel.StockAdjustmentViewModel

@Composable
fun StoreNavigation(user: User, busy: Boolean, error: String?, onLogout: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val database = remember(context) { OylDatabase.getInstance(context) }
    val catalogFactory = remember(database) { CatalogViewModel.Factory(ProductRepository(database, OylRestClient.service)) }
    val cartFactory = remember(database, user.id) { CartViewModel.Factory(CartRepository(database), user.id) }
    val orderFactory = remember(database, user.id) { OrderViewModel.Factory(OrderRepository(database, OylRestClient.service), user) }
    val notificationFactory = remember(database, user.id) {
        NotificationViewModel.Factory(NotificationRepository(database), user.id)
    }
    val notices: NotificationViewModel = viewModel(key = "notices-${user.id}", factory = notificationFactory)
    val noticeState by notices.state.collectAsStateWithLifecycle()
    var permissionGranted by remember { mutableStateOf(DemoLocalNotifier.hasPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionGranted = it }
    val inventoryFactory = remember(database, user.id) {
        InventoryViewModel.Factory(InventoryRepository(database), user.id)
    }
    val catalog: CatalogViewModel = viewModel(key = "catalog-${user.id}", factory = catalogFactory)
    val cart: CartViewModel = viewModel(key = "cart-${user.id}", factory = cartFactory)
    val orders: OrderViewModel = viewModel(key = "orders-${user.id}", factory = orderFactory)
    val inventory: InventoryViewModel? = if (user.role == UserRole.ADMIN)
        viewModel(key = "inventory-${user.id}", factory = inventoryFactory) else null
    val state by catalog.state.collectAsStateWithLifecycle()
    val cartState by cart.state.collectAsStateWithLifecycle()
    val orderState by orders.state.collectAsStateWithLifecycle()
    BackHandler(enabled = orderState.busy) {}
    val inventoryState = if (inventory != null) {
        val observed by inventory.state.collectAsStateWithLifecycle()
        observed
    } else null
    val navigationEnabled = !busy && !orderState.busy && !state.savingProduct && inventoryState?.busy != true
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
            NavigationBarItem(selected = current == "store_home", enabled = navigationEnabled,
                onClick = { openTab("store_home") }, icon = { Icon(Icons.Default.Home, null) },
                label = { Text("Inicio") })
            NavigationBarItem(selected = current == "catalog" || current == "product/{id}", enabled = navigationEnabled,
                onClick = { openTab("catalog") }, icon = { Icon(Icons.Default.ShoppingBag, null) },
                label = { Text("Catálogo") })
            NavigationBarItem(selected = current == "qr_scanner", enabled = navigationEnabled,
                onClick = { openTab("qr_scanner") }, icon = { Icon(Icons.Default.Search, null) },
                label = { Text("QR") })
            if (admin) {
                NavigationBarItem(selected = current == "manage" || current == "editor/{id}" ||
                    current == "inventory" || current == "inventory_adjust/{id}",
                    enabled = navigationEnabled, onClick = { openTab("manage") },
                    icon = { Icon(Icons.Default.Inventory2, null) }, label = { Text("Productos") })
            } else {
                NavigationBarItem(selected = current == "cart", enabled = navigationEnabled,
                    onClick = { cart.reload(); openTab("cart") },
                    icon = { Icon(Icons.Default.ShoppingCart, null) }, label = { Text("Carrito") })
            }
            NavigationBarItem(selected = current == "profile" || current == "orders", enabled = navigationEnabled,
                onClick = { openTab("profile") }, icon = { Icon(Icons.Default.Person, null) },
                label = { Text("Cuenta") })
        }
    }) { padding ->
        NavHost(navController = nav, startDestination = "store_home",
            modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("store_home") {
                LaunchedEffect(user.id, state.loading) {
                    // Releer las fuentes locales al volver a Inicio y tras refrescar el catálogo.
                    if (!state.loading) {
                        cart.reload()
                        orders.load()
                        inventory?.load()
                        notices.load()
                    }
                }
                HomeScreen(user, state, cartState, orderState, inventoryState,
                    onCatalog = { openTab("catalog") }, onProduct = ::openProduct, onRetry = catalog::load,
                    onMap = { nav.navigate("pickup_map") { launchSingleTop = true } },
                    onNotifications = { notices.load(); nav.navigate("notifications") { launchSingleTop = true } },
                    unreadNotices = noticeState.unreadCount,
                    onCart = { if (!admin) { cart.reload(); openTab("cart") } },
                    onOrders = { orders.load(); nav.navigate("orders") { launchSingleTop = true } },
                    onProducts = { if (admin) openTab("manage") },
                    onInventory = { if (admin) { inventory?.load(); nav.navigate("inventory") { launchSingleTop = true } } },
                    onRetryInventory = { inventory?.load() }, navigationEnabled = navigationEnabled)
            }
            composable("pickup_map") {
                PickupMapScreen(onBack = { nav.popBackStack() })
            }
            composable("notifications") {
                NotificationsScreen(noticeState,
                    onBack = { nav.popBackStack() },
                    onRefresh = notices::load,
                    onMarkRead = notices::read,
                    onMarkAllRead = notices::readAll,
                    onOpen = { notice ->
                        when (notice.target) {
                            "orders" -> { orders.load(); nav.navigate("orders") { launchSingleTop = true } }
                            "inventory" -> {
                                if (admin) {
                                    inventory?.load()
                                    nav.navigate("inventory") { launchSingleTop = true }
                                }
                            }
                            else -> Unit
                        }
                    },
                    onTest = { notices.createTest { notice ->
                        DemoLocalNotifier.show(context, notice)
                    } },
                    permissionGranted = permissionGranted,
                    onRequestPermission = {
                        if (android.os.Build.VERSION.SDK_INT >= 33)
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else permissionGranted = true
                    })
            }
            composable("rest_api") {
                val remote: RemoteCatalogViewModel = viewModel(
                    key = "oyl-remote-api",
                    factory = RemoteCatalogViewModel.Factory(OylRestClient.service, context)
                )
                val remoteState by remote.state.collectAsStateWithLifecycle()
                RemoteCatalogScreen(
                    state = remoteState,
                    isAdmin = admin,
                    onBack = { nav.popBackStack() },
                    onRefresh = remote::load,
                    onCreateDemoOrder = remote::createDemoOrder,
                    onNewDemoOrder = remote::prepareAnotherDemoOrder
                )
            }
            composable("catalog") {
                CatalogScreen(state, catalog::search, catalog::selectCategory,
                    catalog::clearFilters, catalog::load, ::openProduct)
            }
            composable("qr_scanner") {
                QrScannerScreen(state, isAdmin = admin,
                    onRefresh = catalog::load,
                    onOpenProduct = ::openProduct,
                    onAdjustInventory = { id ->
                        if (admin) {
                            inventory?.load()
                            nav.navigate("inventory_adjust/$id") { launchSingleTop = true }
                        }
                    })
            }
            composable("product/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { route ->
                val id = route.arguments?.getLong("id")
                ProductDetailScreen(state.products.firstOrNull { it.id == id }, state.loading,
                    state.error, catalog::load, { nav.popBackStack() },
                    isAdmin = admin, onEdit = { if (admin && id != null) nav.navigate("editor/$id") },
                    cartBusy = !cartState.canEdit || orderState.busy,
                    cartNotice = cartState.notice, cartError = cartState.error,
                    onAdd = { quantity -> if (!admin && id != null) cart.add(id, quantity) },
                    onCart = { if (!admin) { cart.reload(); openTab("cart") } })
            }
            if (admin) {
                composable("manage") {
                    ManageProductsScreen(state, { nav.navigate("editor/0") },
                        { nav.navigate("editor/$it") }, catalog::load,
                        onInventory = { inventory?.load(); nav.navigate("inventory") { launchSingleTop = true } })
                }
                composable("inventory") {
                    if (inventoryState != null) {
                        InventoryScreen(inventoryState,
                            onBack = { catalog.load(); nav.popBackStack() },
                            onRefresh = { inventory?.load() },
                            onAdjust = { productId ->
                                inventory?.clearNotice()
                                nav.navigate("inventory_adjust/$productId") { launchSingleTop = true }
                            })
                    }
                }
                composable("inventory_adjust/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })) { route ->
                    val id = route.arguments?.getLong("id")
                    val product = inventoryState?.products?.firstOrNull { it.id == id }
                    if (product != null && inventoryState != null && inventory != null) {
                        val form: StockAdjustmentViewModel = viewModel(
                            key = "stock-form-${product.id}",
                            factory = StockAdjustmentViewModel.Factory(product.stock) { type, quantity, note, done ->
                                val startedAt = System.currentTimeMillis()
                                inventory.adjust(product.id, type, quantity, note, onResult = done) {
                                    catalog.load()
                                    notices.announceIfNew(NoticeKind.STOCK_LOW, startedAt) { created ->
                                        DemoLocalNotifier.show(context, created)
                                    }
                                    nav.popBackStack()
                                }
                            })
                        StockAdjustmentScreen(product, form, onBack = { nav.popBackStack() })
                    } else {
                        Column(Modifier.padding(20.dp)) {
                            Text("Producto no encontrado en el inventario")
                            TextButton(onClick = { nav.popBackStack() }) { Text("Volver") }
                        }
                    }
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
                        val form: ProductFormViewModel = viewModel(
                            key = "product-form-$id",
                            factory = ProductFormViewModel.Factory(product, id == 0L) { draft, done ->
                                catalog.saveProduct(id.takeUnless { it == 0L }, draft, done)
                            })
                        EditProductScreen(product, form, onBack = { nav.popBackStack() })
                    }
                }
            } else {
                composable("cart") {
                    LaunchedEffect(Unit) { cart.reload() }
                    CartScreen(cartState, { id, quantity -> cart.change(id, quantity) },
                        cart::reload, { openTab("catalog") },
                        { cart.reload(); nav.navigate("order_confirmation") })
                }
            }
            if (!admin) {
                composable("order_confirmation") {
                    LaunchedEffect(Unit) { cart.reload() }
                    OrderConfirmationScreen(user, cartState, orderState,
                        onBack = { nav.popBackStack() },
                        onConfirm = { delivery: DeliveryMethod ->
                            val startedAt = System.currentTimeMillis()
                            orders.place(delivery, cartState.items, onSettled = cart::reload) {
                                cart.reload()
                                catalog.load()
                                notices.announceIfNew(NoticeKind.ORDER_CREATED, startedAt) { created ->
                                    DemoLocalNotifier.show(context, created)
                                }
                                nav.navigate("order_success") {
                                    popUpTo("cart") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        })
                }
                composable("order_success") {
                    OrderSuccessScreen(orderState.placedId,
                        serverOrderId = orderState.placedServerId,
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
                    onOrdersClick = { orders.load(); nav.navigate("orders") { launchSingleTop = true } },
                    onMapClick = { nav.navigate("pickup_map") { launchSingleTop = true } },
                    onNotificationsClick = {
                        notices.load()
                        nav.navigate("notifications") { launchSingleTop = true }
                    }, unreadNotices = noticeState.unreadCount,
                    onRestClick = { nav.navigate("rest_api") { launchSingleTop = true } })
            }
        }
    }
}
