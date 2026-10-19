package com.example.oylfrutossecos.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.oylfrutossecos.data.model.Product
import com.example.oylfrutossecos.data.model.CartItem
import com.example.oylfrutossecos.data.model.ProductDraft
import com.example.oylfrutossecos.data.model.User
import com.example.oylfrutossecos.data.model.OrderSummary
import com.example.oylfrutossecos.data.model.OrderLine
import com.example.oylfrutossecos.data.model.OrderStatus
import com.example.oylfrutossecos.data.model.DeliveryMethod
import com.example.oylfrutossecos.data.model.Registration
import com.example.oylfrutossecos.data.model.UserRole
import com.example.oylfrutossecos.domain.AuthValidator
import com.example.oylfrutossecos.domain.InventoryRules
import com.example.oylfrutossecos.data.model.InventoryMovement
import com.example.oylfrutossecos.data.model.MovementType
import com.example.oylfrutossecos.data.model.AppNotice
import com.example.oylfrutossecos.data.model.NoticeKind
import com.example.oylfrutossecos.data.remote.ApiOrderResponse
import com.example.oylfrutossecos.data.remote.ApiProduct

// Estos métodos se invocan desde Dispatchers.IO en AuthRepository.
class OylDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "oyl_auth.db", null, 7) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                alias TEXT NOT NULL UNIQUE,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                address TEXT NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('CLIENT', 'ADMIN')),
                password_salt BLOB NOT NULL,
                password_hash BLOB NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE session (
                id INTEGER PRIMARY KEY CHECK(id = 1),
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Cuenta ficticia para la demostración académica.
        val admin = Registration(
            name = "Administrador", alias = "admin", email = "admin@admin.cl",
            phone = "+56 9 1234 1234", address = "Santiago", password = "Admin1234"
        )
        insertUser(db, admin, UserRole.ADMIN)
        createProducts(db)
        createCartTable(db)
        createOrderTables(db)
        createRemoteCheckoutStorage(db)
        createInventoryTables(db)
        createNoticeTable(db)
        seedLowStockNotices(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migración 1 → 2: conserva users y session, agrega solamente products.
        if (oldVersion < 2) createProducts(db)
        if (oldVersion < 3) createCartTable(db)
        if (oldVersion < 4) createOrderTables(db)
        if (oldVersion < 5) createInventoryTables(db)
        if (oldVersion < 7) createRemoteCheckoutStorage(db)
        if (oldVersion < 6) {
            createNoticeTable(db)
            seedLowStockNotices(db)
        }
    }


    private fun createProducts(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE products (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT NOT NULL,
                price_clp INTEGER NOT NULL CHECK(price_clp > 0),
                weight_grams INTEGER NOT NULL CHECK(weight_grams > 0),
                stock INTEGER NOT NULL CHECK(stock >= 0),
                image_key TEXT NOT NULL
            )
        """.trimIndent())
        // Semilla inicial ficticia. Se ejecuta una vez, no cada vez que abres la app.
        val examples = listOf(
            Product(1, "Almendras naturales", "Frutos secos", "Almendras naturales en bolsa de 250 gramos. Una opción para acompañar tus preparaciones.", 3490, 250, 20, "nut"),
            Product(2, "Nueces mariposa", "Frutos secos", "Nueces en mitades, en bolsa de 250 gramos. Para repostería y otras preparaciones.", 3290, 250, 12, "nut"),
            Product(3, "Maní tostado", "Frutos secos", "Maní tostado en bolsa de 500 gramos, para compartir o usar en tus recetas.", 2490, 500, 30, "nut"),
            Product(4, "Castañas de cajú", "Frutos secos", "Castañas de cajú en presentación de 250 gramos.", 4490, 250, 0, "nut"),
            Product(5, "Pasas rubias", "Deshidratados", "Pasas rubias en bolsa de 250 gramos, para repostería y mezclas.", 1990, 250, 18, "fruit"),
            Product(6, "Cranberries", "Deshidratados", "Cranberries deshidratados en presentación de 250 gramos.", 2790, 250, 8, "fruit"),
            Product(7, "Semillas de chía", "Semillas", "Semillas de chía en bolsa de 250 gramos para incorporar en tus preparaciones.", 2290, 250, 15, "seed"),
            Product(8, "Semillas de zapallo", "Semillas", "Semillas de zapallo en presentación de 250 gramos.", 2990, 250, 6, "seed")
        )
        examples.forEach { product ->
            db.insertOrThrow("products", null, ContentValues().apply {
                put("id", product.id)
                put("name", product.name)
                put("category", product.category)
                put("description", product.description)
                put("price_clp", product.priceClp)
                put("weight_grams", product.weightGrams)
                put("stock", product.stock)
                put("image_key", product.imageKey)
            })
        }
    }

    fun readProducts(): List<Product> {
        val result = mutableListOf<Product>()
        readableDatabase.query("products", null, null, null, null, null, "id ASC").use { cursor ->
            while (cursor.moveToNext()) {
                result.add(Product(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    priceClp = cursor.getInt(cursor.getColumnIndexOrThrow("price_clp")),
                    weightGrams = cursor.getInt(cursor.getColumnIndexOrThrow("weight_grams")),
                    stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock")),
                    imageKey = cursor.getString(cursor.getColumnIndexOrThrow("image_key"))
                ))
            }
        }
        return result
    }

    private fun createCartTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS cart_items (
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                quantity INTEGER NOT NULL CHECK(quantity > 0),
                PRIMARY KEY(user_id, product_id)
            )
        """.trimIndent())
    }

    // El carrito es individual por usuario, y no modifica las existencias del catálogo.
    fun readCart(userId: Long): List<CartItem> {
        val products = readProducts().associateBy { it.id }
        val items = mutableListOf<CartItem>()
        readableDatabase.query("cart_items", arrayOf("product_id", "quantity"),
            "user_id = ?", arrayOf(userId.toString()), null, null, "product_id ASC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    val product = products[cursor.getLong(0)] ?: continue
                    items.add(CartItem(product, cursor.getInt(1)))
                }
            }
        return items
    }

    fun setCartQuantity(userId: Long, productId: Long, quantity: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val pendingOrder = db.rawQuery(
                "SELECT request_id FROM pending_remote_checkout WHERE user_id = ?",
                arrayOf(userId.toString())
            ).use { it.moveToFirst() }
            check(!pendingOrder) {
                "Tienes un pedido remoto pendiente. Vuelve a Confirmar pedido y reintenta antes de modificar el carrito."
            }
            if (quantity <= 0) {
                db.delete("cart_items", "user_id = ? AND product_id = ?",
                    arrayOf(userId.toString(), productId.toString()))
            } else {
                db.rawQuery("SELECT stock FROM products WHERE id = ?",
                    arrayOf(productId.toString())).use { cursor ->
                    require(cursor.moveToFirst()) { "El producto ya no existe." }
                    require(quantity <= cursor.getInt(0)) { "No hay suficiente stock para esa cantidad." }
                }
                val row = ContentValues().apply {
                    put("user_id", userId)
                    put("product_id", productId)
                    put("quantity", quantity)
                }
                check(db.insertWithOnConflict("cart_items", null, row,
                    SQLiteDatabase.CONFLICT_REPLACE) != -1L) { "No se pudo guardar el carrito." }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun saveProduct(id: Long?, draft: ProductDraft): Long {
        require(draft.validationError() == null) { draft.validationError().orEmpty() }
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", draft.name.trim())
            put("category", draft.category.trim())
            put("description", draft.description.trim())
            put("price_clp", draft.priceClp)
            put("weight_grams", draft.weightGrams)
            put("stock", draft.stock)
        }
        return if (id == null) {
            values.put("image_key", draft.imageKey ?: when (draft.category) {
                "Semillas" -> "seed"
                "Deshidratados" -> "fruit"
                else -> "nut"
            })
            val inserted = db.insertOrThrow("products", null, values)
            check(inserted > 0L) { "No se pudo crear el producto." }
            inserted
        } else {
            // El inventario se modifica solo mediante movimientos auditables.
            // Aunque una pantalla antigua envíe stock, no lo sobrescribimos aquí.
            values.remove("stock")
            // La fotografía se guarda en el almacenamiento privado de la aplicación.
            if (draft.imageKey != null) values.put("image_key", draft.imageKey)
            check(db.update("products", values, "id = ?", arrayOf(id.toString())) == 1) {
                "El producto no existe."
            }
            id
        }
    }


    private fun createRemoteCheckoutStorage(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE demo_orders ADD COLUMN server_order_id TEXT")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_remote_id ON demo_orders(server_order_id)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS pending_remote_checkout (
                user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                request_id TEXT NOT NULL UNIQUE,
                request_json TEXT NOT NULL,
                delivery TEXT NOT NULL,
                cart_fingerprint TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
    }

    private fun createOrderTables(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS demo_orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                customer_name TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                status TEXT NOT NULL,
                delivery TEXT NOT NULL,
                phone TEXT NOT NULL,
                address TEXT NOT NULL,
                total_clp INTEGER NOT NULL CHECK(total_clp > 0)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS demo_order_lines (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id INTEGER NOT NULL REFERENCES demo_orders(id) ON DELETE CASCADE,
                product_id INTEGER NOT NULL,
                product_name TEXT NOT NULL,
                quantity INTEGER NOT NULL CHECK(quantity > 0),
                unit_price_clp INTEGER NOT NULL CHECK(unit_price_clp > 0)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_demo_orders_user ON demo_orders(user_id)")
    }

    /** Guarda una SOLICITUD LOCAL, no una compra ni reserva de stock real. */
    fun placeDemoOrder(user: User, delivery: DeliveryMethod, expectedItems: List<CartItem>): Long {
        require(user.role == UserRole.CLIENT) { "Solo clientes pueden crear pedidos." }
        require(expectedItems.isNotEmpty()) { "Tu carrito está vacío." }
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Consulta en esta misma transacción los precios, cantidades y stock actuales.
            val lines = mutableListOf<OrderLine>()
            db.rawQuery("""
                SELECT p.id, p.name, p.price_clp, p.stock, c.quantity
                FROM cart_items c JOIN products p ON p.id = c.product_id
                WHERE c.user_id = ? ORDER BY p.id ASC
            """.trimIndent(), arrayOf(user.id.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    val quantity = cursor.getInt(4)
                    val stock = cursor.getInt(3)
                    require(quantity in 1..stock) {
                        "Stock insuficiente de ${cursor.getString(1)}. Regresa al carrito."
                    }
                    lines.add(OrderLine(cursor.getLong(0), cursor.getString(1),
                        quantity, cursor.getInt(2)))
                }
            }
            require(lines.isNotEmpty()) { "El carrito ya está vacío." }
            val expected = expectedItems.associate { it.product.id to (it.quantity to it.product.priceClp) }
            require(lines.size == expected.size && lines.all {
                expected[it.productId] == (it.quantity to it.priceClp)
            }) { "El carrito o algún precio cambió. Regresa al carrito para revisar." }
            val total = lines.sumOf { it.subtotalClp }
            check(total in 1..Int.MAX_VALUE.toLong()) { "El total del pedido es demasiado alto." }
            val orderId = db.insertOrThrow("demo_orders", null, ContentValues().apply {
                put("user_id", user.id)
                put("customer_name", user.name)
                put("created_at", System.currentTimeMillis())
                put("status", OrderStatus.PENDING.name)
                put("delivery", delivery.name)
                put("phone", user.phone)
                put("address", if (delivery == DeliveryMethod.DELIVERY) user.address else "Retiro en tienda")
                put("total_clp", total)
            })
            check(orderId > 0L) { "No se pudo generar el pedido." }
            lines.forEach { line ->
                db.insertOrThrow("demo_order_lines", null, ContentValues().apply {
                    put("order_id", orderId)
                    put("product_id", line.productId)
                    put("product_name", line.name)
                    put("quantity", line.quantity)
                    put("unit_price_clp", line.priceClp)
                })
            }
            db.delete("cart_items", "user_id = ?", arrayOf(user.id.toString()))
            addNotice(db, user.id, NoticeKind.ORDER_CREATED,
                "Pedido #$orderId registrado", "Tu solicitud de prueba quedó pendiente de validación.", "orders")
            forEachAdmin(db) { adminId ->
                addNotice(db, adminId, NoticeKind.ORDER_CREATED,
                    "Nuevo pedido #$orderId", "${user.name} registró un pedido de prueba por CLP $total.", "orders")
            }
            db.setTransactionSuccessful()
            return orderId
        } finally {
            db.endTransaction()
        }
    }

    fun readOrders(userId: Long?): List<OrderSummary> {
        val db = readableDatabase
        val where = if (userId == null) null else "user_id = ?"
        val args = if (userId == null) null else arrayOf(userId.toString())
        val result = mutableListOf<OrderSummary>()
        db.query("demo_orders", null, where, args, null, null, "created_at DESC, id DESC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    val orderId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                    val lines = mutableListOf<OrderLine>()
                    db.query("demo_order_lines", null, "order_id = ?",
                        arrayOf(orderId.toString()), null, null, "id ASC").use { lc ->
                        while (lc.moveToNext()) {
                            lines.add(OrderLine(
                                lc.getLong(lc.getColumnIndexOrThrow("product_id")),
                                lc.getString(lc.getColumnIndexOrThrow("product_name")),
                                lc.getInt(lc.getColumnIndexOrThrow("quantity")),
                                lc.getInt(lc.getColumnIndexOrThrow("unit_price_clp"))
                            ))
                        }
                    }
                    result.add(OrderSummary(
                        id = orderId,
                        userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                        customerName = cursor.getString(cursor.getColumnIndexOrThrow("customer_name")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                        status = OrderStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                        delivery = DeliveryMethod.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("delivery"))),
                        contactPhone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                        contactAddress = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                        totalClp = cursor.getLong(cursor.getColumnIndexOrThrow("total_clp")),
                        lines = lines,
                        remoteOrderId = cursor.getString(cursor.getColumnIndexOrThrow("server_order_id"))
                    ))
                }
            }
        return result
    }

    fun advanceDemoOrder(orderId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val (current, clientId) = db.rawQuery("SELECT status, user_id, server_order_id FROM demo_orders WHERE id = ?",
                arrayOf(orderId.toString())).use { cursor ->
                check(cursor.moveToFirst()) { "No se encontró el pedido." }
                check(cursor.isNull(2)) { "El estado de un pedido remoto se gestiona en Spring Boot." }
                OrderStatus.valueOf(cursor.getString(0)) to cursor.getLong(1)
            }
            val next = current.next() ?: error("Este pedido ya se entregó.")
            val values = ContentValues().apply { put("status", next.name) }
            check(db.update("demo_orders", values, "id = ?", arrayOf(orderId.toString())) == 1) {
                "No se pudo cambiar el estado."
            }
            addNotice(db, clientId, NoticeKind.ORDER_STATUS,
                "Pedido #$orderId: ${next.displayName}", "El estado de tu pedido de prueba cambió.", "orders")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }


    private fun createInventoryTables(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS inventory_movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER NOT NULL REFERENCES products(id),
                admin_user_id INTEGER NOT NULL REFERENCES users(id),
                movement_type TEXT NOT NULL CHECK(movement_type IN ('ENTRY','EXIT')),
                quantity INTEGER NOT NULL CHECK(quantity > 0),
                before_stock INTEGER NOT NULL CHECK(before_stock >= 0),
                after_stock INTEGER NOT NULL CHECK(after_stock >= 0),
                note TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_inventory_product ON inventory_movements(product_id, id DESC)")
    }

    /** Modifica EXISTENCIAS LOCALES de demostración y deja una auditoría en una transacción. */
    fun recordInventoryMovement(adminId: Long, productId: Long, type: MovementType,
                                quantity: Int, note: String): Long {
        InventoryRules.validateNote(note)?.let { throw IllegalArgumentException(it) }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val validAdmin = db.rawQuery("SELECT role FROM users WHERE id = ?",
                arrayOf(adminId.toString())).use { it.moveToFirst() && it.getString(0) == "ADMIN" }
            require(validAdmin) { "Solo el administrador puede actualizar el inventario." }
            val oldStock = db.rawQuery("SELECT stock FROM products WHERE id = ?",
                arrayOf(productId.toString())).use { cursor ->
                require(cursor.moveToFirst()) { "El producto ya no existe." }
                cursor.getInt(0)
            }
            val newStock = InventoryRules.newStock(oldStock, type, quantity)
            val values = ContentValues().apply { put("stock", newStock) }
            check(db.update("products", values, "id = ? AND stock = ?",
                arrayOf(productId.toString(), oldStock.toString())) == 1) {
                "El stock cambió. Actualiza y vuelve a intentar."
            }
            val movementId = db.insertOrThrow("inventory_movements", null, ContentValues().apply {
                put("product_id", productId)
                put("admin_user_id", adminId)
                put("movement_type", type.name)
                put("quantity", quantity)
                put("before_stock", oldStock)
                put("after_stock", newStock)
                put("note", note.trim())
                put("created_at", System.currentTimeMillis())
            })
            check(movementId > 0L) { "No se pudo registrar el movimiento." }
            if (newStock <= 10 && (oldStock > 10 || (newStock == 0 && oldStock > 0))) {
                val productName = db.rawQuery("SELECT name FROM products WHERE id = ?",
                    arrayOf(productId.toString())).use { cursor ->
                    check(cursor.moveToFirst()) { "Producto no encontrado." }
                    cursor.getString(0)
                }
                addNotice(db, adminId, NoticeKind.STOCK_LOW,
                    if (newStock == 0) "Producto agotado" else "Alerta de stock bajo",
                    "$productName: quedan $newStock bolsas (demo).", "inventory")
            }
            db.setTransactionSuccessful()
            return movementId
        } finally { db.endTransaction() }
    }

    fun readInventoryMovements(productId: Long? = null): List<InventoryMovement> {
        val result = mutableListOf<InventoryMovement>()
        val where = if (productId != null) "WHERE m.product_id = ?" else ""
        val args = if (productId != null) arrayOf(productId.toString()) else null
        readableDatabase.rawQuery("""
            SELECT m.id, m.product_id, p.name, m.movement_type, m.quantity,
                   m.before_stock, m.after_stock, m.note, m.created_at
            FROM inventory_movements m JOIN products p ON p.id = m.product_id
            $where ORDER BY m.id DESC LIMIT 100
        """.trimIndent(), args).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(InventoryMovement(
                    id = cursor.getLong(0), productId = cursor.getLong(1),
                    productName = cursor.getString(2),
                    type = MovementType.valueOf(cursor.getString(3)),
                    quantity = cursor.getInt(4), beforeStock = cursor.getInt(5),
                    afterStock = cursor.getInt(6), note = cursor.getString(7),
                    createdAt = cursor.getLong(8)
                ))
            }
        }
        return result
    }



    // Paso 9: avisos LOCALES y separados por usuario. Las notificaciones push requieren backend.
    private fun createNoticeTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS app_notices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                kind TEXT NOT NULL,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                target TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                read_at INTEGER
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_notices_user ON app_notices(user_id, id DESC)")
    }

    private fun addNotice(db: SQLiteDatabase, userId: Long, kind: NoticeKind,
                          title: String, message: String, target: String): Long =
        db.insertOrThrow("app_notices", null, ContentValues().apply {
            put("user_id", userId)
            put("kind", kind.name)
            put("title", title)
            put("message", message)
            put("target", target)
            put("created_at", System.currentTimeMillis())
        })

    private fun forEachAdmin(db: SQLiteDatabase, consumer: (Long) -> Unit) {
        db.rawQuery("SELECT id FROM users WHERE role = 'ADMIN'", null).use { c ->
            while (c.moveToNext()) consumer(c.getLong(0))
        }
    }

    // Únicamente al CREAR o migrar la tabla: registra alertas de stock bajo preexistentes.
    private fun seedLowStockNotices(db: SQLiteDatabase) {
        forEachAdmin(db) { adminId ->
            db.rawQuery("SELECT name, stock FROM products WHERE stock <= 10 ORDER BY id", null)
                .use { c ->
                    while (c.moveToNext()) {
                        val stock = c.getInt(1)
                        addNotice(db, adminId, NoticeKind.STOCK_LOW,
                            if (stock == 0) "Producto agotado" else "Stock bajo (demo)",
                            "${c.getString(0)}: $stock bolsas disponibles.", "inventory")
                    }
                }
        }
    }

    fun readNotices(userId: Long): List<AppNotice> {
        val notices = mutableListOf<AppNotice>()
        readableDatabase.query("app_notices", null, "user_id = ?", arrayOf(userId.toString()),
            null, null, "id DESC", "100").use { c ->
            while (c.moveToNext()) notices.add(AppNotice(
                id = c.getLong(c.getColumnIndexOrThrow("id")),
                userId = c.getLong(c.getColumnIndexOrThrow("user_id")),
                kind = NoticeKind.valueOf(c.getString(c.getColumnIndexOrThrow("kind"))),
                title = c.getString(c.getColumnIndexOrThrow("title")),
                message = c.getString(c.getColumnIndexOrThrow("message")),
                target = c.getString(c.getColumnIndexOrThrow("target")),
                createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
                readAt = c.getColumnIndexOrThrow("read_at").let { i ->
                    if (c.isNull(i)) null else c.getLong(i)
                }
            ))
        }
        return notices
    }

    fun markNoticeRead(userId: Long, noticeId: Long) {
        writableDatabase.execSQL(
            "UPDATE app_notices SET read_at = ? WHERE id = ? AND user_id = ? AND read_at IS NULL",
            arrayOf(System.currentTimeMillis(), noticeId, userId))
    }

    fun markAllNoticesRead(userId: Long) {
        writableDatabase.execSQL(
            "UPDATE app_notices SET read_at = ? WHERE user_id = ? AND read_at IS NULL",
            arrayOf(System.currentTimeMillis(), userId))
    }

    fun addTestNotice(userId: Long): AppNotice {
        val db = writableDatabase
        val id = addNotice(db, userId, NoticeKind.TEST, "Aviso de prueba OYL",
            "Las notificaciones locales funcionan. No es un mensaje enviado desde Internet.", "none")
        return readNotices(userId).first { it.id == id }
    }

    private fun insertUser(db: SQLiteDatabase, form: Registration, role: UserRole): Long {
        val salt = PasswordHasher.newSalt()
        return db.insertOrThrow("users", null, ContentValues().apply {
            put("name", form.name.trim())
            put("alias", AuthValidator.normalizeAlias(form.alias))
            put("email", form.email.trim())
            put("phone", form.phone.trim())
            put("address", form.address.trim())
            put("role", role.name)
            put("password_salt", salt)
            put("password_hash", PasswordHasher.hash(form.password, salt))
        })
    }

    fun registerClient(form: Registration): Long =
        insertUser(writableDatabase, form, UserRole.CLIENT)

    fun authenticate(alias: String, password: String, role: UserRole): User? {
        readableDatabase.query(
            "users", null, "alias = ? AND role = ?",
            arrayOf(AuthValidator.normalizeAlias(alias), role.name), null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val salt = cursor.getBlob(cursor.getColumnIndexOrThrow("password_salt"))
            val hash = cursor.getBlob(cursor.getColumnIndexOrThrow("password_hash"))
            return if (PasswordHasher.verify(password, salt, hash)) cursor.toUser() else null
        }
    }

    fun saveSession(userId: Long) {
        writableDatabase.insertWithOnConflict("session", null, ContentValues().apply {
            put("id", 1)
            put("user_id", userId)
        }, SQLiteDatabase.CONFLICT_REPLACE).also {
            check(it != -1L) { "No se pudo guardar la sesión" }
        }
    }

    fun readSession(): User? {
        readableDatabase.rawQuery(
            "SELECT u.* FROM users u JOIN session s ON s.user_id = u.id WHERE s.id = 1",
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toUser() else null
        }
    }

    fun clearSession() {
        writableDatabase.delete("session", null, null)
    }

    private fun Cursor.toUser() = User(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        alias = getString(getColumnIndexOrThrow("alias")),
        email = getString(getColumnIndexOrThrow("email")),
        phone = getString(getColumnIndexOrThrow("phone")),
        address = getString(getColumnIndexOrThrow("address")),
        role = UserRole.valueOf(getString(getColumnIndexOrThrow("role")))
    )

    companion object {
        @Volatile private var instance: OylDatabase? = null

        fun getInstance(context: Context): OylDatabase = instance ?: synchronized(this) {
            instance ?: OylDatabase(context.applicationContext).also { instance = it }
        }
    }
}
