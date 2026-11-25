package com.example.oylfrutossecos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.*
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Before
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
abstract class LogicViewModelTest {
    protected val main = StandardTestDispatcher()
    private val models = ViewModelStore()
    private var sequence = 0
    @Before fun installMainDispatcher() { Dispatchers.setMain(main) }
    @After fun releaseViewModels() {
        models.clear()
        Dispatchers.resetMain()
        unmockkAll()
    }
    protected fun <T : ViewModel> create(factory: ViewModelProvider.Factory, type: Class<T>): T =
        ViewModelProvider(models, factory).get("test-${sequence++}", type)
}

internal object LogicFixtures {
    val client = User(7, "Cliente de prueba", "cliente", "cliente@example.test", "912345678", "Calle de prueba 123", UserRole.CLIENT)
    val admin = client.copy(id = 1, alias = "admin", role = UserRole.ADMIN)
    val product = Product(41, "Almendras", "Frutos secos", "Bolsa seleccionada", 3000, 250, 5, "nut", remoteId = 1)
    val cart = listOf(CartItem(product, 2))
    val draft = ProductDraft("Almendras", "Frutos secos", "Bolsa seleccionada", 3000, 250, 5, "nut")
    val order = OrderSummary(11, client.id, client.name, 100, OrderStatus.PENDING,
        DeliveryMethod.PICKUP, client.phone, "Retiro en tienda", 6000,
        listOf(OrderLine(product.id, product.name, 2, 3000)), "remote-11")
    fun notice(id: Long = 1, kind: NoticeKind = NoticeKind.TEST, at: Long = 100, read: Boolean = false) =
        AppNotice(id, client.id, kind, "Aviso", "Mensaje", "orders", at, if (read) 200 else null)
    fun http(code: Int) = HttpException(Response.error<Any>(code, ResponseBody.create(null, "private detail")))
}
