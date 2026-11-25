package com.example.oylfrutossecos

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import com.example.oylfrutossecos.data.local.OylDatabase
import com.example.oylfrutossecos.data.model.*
import com.example.oylfrutossecos.data.remote.*
import com.google.gson.Gson
import java.io.File
import java.util.UUID
import okhttp3.RequestBody
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.robolectric.RuntimeEnvironment

/** JVM-only SQLite sandbox. Never opens the app singleton, emulator or user's database. */
abstract class RepositorySqliteSupport {
    protected lateinit var db: OylDatabase
    private lateinit var sandboxContext: Context
    protected val api = RepositoryApi()
    protected val key = "unit-test-administrative-key"
    @Before fun openSandbox() {
        val base:Context=RuntimeEnvironment.getApplication()
        // Keep paths short: sqlite4java's Windows WAL files can hit MAX_PATH in Robolectric's cache.
        val file=File(java.nio.file.Files.createTempDirectory("oyl-").toFile(),"test.db")
        val context=object:ContextWrapper(base) {
            override fun getApplicationContext():Context=this
            override fun getDatabasePath(name:String)=file
            override fun openOrCreateDatabase(name:String,mode:Int,factory:SQLiteDatabase.CursorFactory?)=
                SQLiteDatabase.openOrCreateDatabase(file,factory)
            override fun openOrCreateDatabase(name:String,mode:Int,factory:SQLiteDatabase.CursorFactory?,handler:DatabaseErrorHandler?)=
                SQLiteDatabase.openDatabase(file.absolutePath,factory,SQLiteDatabase.CREATE_IF_NECESSARY,handler)
        }
        sandboxContext=context
        db=newHelper()
        db.writableDatabase // initialize only the isolated file
    }
    private fun newHelper():OylDatabase = OylDatabase::class.java.getDeclaredConstructor(Context::class.java)
        .apply {isAccessible=true}.newInstance(sandboxContext)
    protected fun reopenSandbox() {
        db.close()
        db=newHelper()
    }
    @After fun closeSandbox() { if(::db.isInitialized) db.close() }
    protected fun user(alias:String="cliente"):User {
        val form=Registration("Cliente Prueba",alias,"$alias@example.test","912345678","Calle de prueba 123","Cliente123","Cliente123")
        val id=db.registerClient(form)
        return User(id,form.name,form.alias,form.email,form.phone,form.address,UserRole.CLIENT)
    }
}

class RepositoryApi:StubOylRestService() {
    var catalog=mutableListOf(ApiProduct(1,"Almendras naturales","Frutos secos","Bolsa de almendras",3000,250,5,"nut"))
    var productsFailure:Exception?=null
    var readFailure:Exception?=null
    var writeFailure:Exception?=null
    var loseOrderResponse=false
    var inventory=listOf<ApiInventoryMovement>()
    val orders=linkedMapOf<String,ApiOrderResponse>()
    val queriedOrders=mutableListOf<String>()
    val productInputs=mutableListOf<ApiProductInput>()
    val stockChanges=mutableListOf<Pair<Long,ApiStockChange>>()
    val submittedJson=mutableListOf<String>()
    var productsCalls=0
    var lastKey:String?=null
    override suspend fun products():List<ApiProduct> {productsCalls++;productsFailure?.let {throw it};return catalog.toList()}
    override suspend fun createProduct(product:ApiProductInput,key:String):ApiProduct {
        lastKey=key;writeFailure?.let {throw it};productInputs+=product
        return ApiProduct(1000,product.name,product.category,product.description,product.priceClp,product.weightGrams,
            product.stock,product.imageKey?:"seed").also {catalog.add(it)}
    }
    override suspend fun editProduct(id:Long,product:ApiProductInput,key:String):ApiProduct {
        lastKey=key;writeFailure?.let {throw it};productInputs+=product
        val original=catalog.single {it.id==id}
        return original.copy(name=product.name,description=product.description,priceClp=product.priceClp,
            imageKey=product.imageKey?:original.imageKey).also { catalog[catalog.indexOf(original)]=it }
    }
    override suspend fun inventoryMovements(key:String):List<ApiInventoryMovement> {
        lastKey=key;readFailure?.let {throw it};return inventory
    }
    override suspend fun adjustStock(id:Long,change:ApiStockChange,key:String):ApiStockResult {
        lastKey=key;writeFailure?.let {throw it};stockChanges+=id to change
        val old=catalog.single {it.id==id};val updated=old.stock+change.delta
        if(updated<0) throw LogicFixtures.http(409)
        catalog[catalog.indexOf(old)]=old.copy(stock=updated)
        return ApiStockResult(id,old.stock,updated)
    }
    override suspend fun adminOrders(key:String):List<ApiOrderResponse> {
        lastKey=key;readFailure?.let {throw it};return orders.values.toList()
    }
    override suspend fun getOrder(id:String):ApiOrderResponse {
        queriedOrders+=id;readFailure?.let {throw it};return orders.values.single {it.id==id}
    }
    override suspend fun advanceOrder(id:String,key:String):ApiOrderResponse {
        lastKey=key;writeFailure?.let {throw it};val old=orders.values.single {it.id==id}
        return old.copy(status=OrderStatus.valueOf(old.status).next()!!.name).also {orders[old.requestId]=it}
    }
    override suspend fun createPersistedOrder(request:RequestBody):ApiOrderResponse {
        val raw=Buffer().also {request.writeTo(it)}.readUtf8();submittedJson+=raw
        val input=Gson().fromJson(raw,ApiOrderRequest::class.java)
        writeFailure?.let {throw it}
        val saved=orders.getOrPut(input.requestId) {
            val lines=input.items.map {line ->
                val product=catalog.single {it.id==line.productId}
                catalog[catalog.indexOf(product)]=product.copy(stock=product.stock-line.quantity)
                ApiOrderItem(product.id,product.name,line.quantity,product.priceClp)
            }
            ApiOrderResponse(UUID.randomUUID().toString(),input.requestId,input.customerAlias,"PENDING",
                lines.sumOf {it.quantity.toLong()*it.unitPriceClp},lines,input.delivery,input.contactPhone,input.contactAddress,100)
        }
        if(loseOrderResponse) throw java.io.IOException("lost after commit")
        return saved
    }
}
