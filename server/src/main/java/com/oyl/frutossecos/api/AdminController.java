package com.oyl.frutossecos.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Endpoints administrativos de desarrollo. Nunca incrustar la clave en el APK.
 * En producción sustituir por un sistema real de autenticación/autorización.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final JdbcTemplate jdbc;
    private final OrderService orders;
    private final String key;

    public AdminController(JdbcTemplate jdbc, OrderService orders,
                           @Value("${OYL_ADMIN_KEY:}") String key) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.key = key;
    }

    void authorize(String candidate) {
        if (key == null || key.length() < 16 || candidate == null
                || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),
                        candidate.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Clave de administrador no configurada o incorrecta");
        }
    }

    public record StockChange(int delta, String note) {}
    public record StockResult(long productId, int oldStock, int newStock) {}
    public record ProductInput(String name, String category, String description,
                               Integer priceClp, Integer weightGrams, Integer stock,
                               String imageKey) {}
    public record Movement(long id, long productId, String productName, int delta,
                           int beforeStock, int afterStock, String note, long createdAt) {}

    private void validateProduct(ProductInput product) {
        if (product == null || product.name() == null || product.name().trim().length() < 3
                || product.name().trim().length() > 150 || product.description() == null
                || product.description().trim().length() < 5 || product.description().length() > 1000
                || product.category() == null
                || !java.util.List.of("Frutos secos", "Semillas", "Deshidratados").contains(product.category())
                || product.priceClp() == null || product.priceClp() <= 0
                || product.weightGrams() == null || product.weightGrams() <= 0
                || product.stock() == null || product.stock() < 0
                || (product.imageKey() != null
                    && !java.util.List.of("nut", "fruit", "seed").contains(product.imageKey()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de producto inválidos");
        }
    }

    private ProductController.ProductDto product(long id) {
        return jdbc.queryForObject("SELECT * FROM products WHERE id = ?", (rs, i) ->
                new ProductController.ProductDto(rs.getLong("id"), rs.getString("name"),
                    rs.getString("category"), rs.getString("description"), rs.getInt("price_clp"),
                    rs.getInt("weight_grams"), rs.getInt("stock"), rs.getString("image_key")), id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProductController.ProductDto createProduct(
            @RequestHeader(value = "X-OYL-ADMIN-KEY", required = false) String suppliedKey,
            @RequestBody ProductInput input) {
        authorize(suppliedKey);
        validateProduct(input);
        long id;
        do { id = jdbc.queryForObject("SELECT NEXT VALUE FOR product_ids", Long.class); }
        while (jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE id = ?", Integer.class, id) != 0);
        String image = input.imageKey() != null ? input.imageKey() : switch (input.category()) {
            case "Semillas" -> "seed";
            case "Deshidratados" -> "fruit";
            default -> "nut";
        };
        jdbc.update("""
                INSERT INTO products(id, name, category, description, price_clp, weight_grams, stock, image_key)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, input.name().trim(), input.category(), input.description().trim(),
                input.priceClp(), input.weightGrams(), input.stock(), image);
        if (input.stock() > 0) jdbc.update("""
                INSERT INTO inventory_movements(product_id, change_qty, before_stock, after_stock, note, created_at)
                VALUES (?, ?, 0, ?, 'Stock inicial', ?)
                """, id, input.stock(), input.stock(), System.currentTimeMillis());
        return product(id);
    }

    @PutMapping("/products/{id}")
    @Transactional
    public ProductController.ProductDto editProduct(@PathVariable long id,
            @RequestHeader(value = "X-OYL-ADMIN-KEY", required = false) String suppliedKey,
            @RequestBody ProductInput input) {
        authorize(suppliedKey);
        validateProduct(input);
        // El stock solo cambia por movimientos; ni un formulario obsoleto lo sobrescribe.
        int changed = jdbc.update("""
                UPDATE products SET name = ?, category = ?, description = ?, price_clp = ?,
                    weight_grams = ?, image_key = COALESCE(?, image_key) WHERE id = ?
                """, input.name().trim(), input.category(), input.description().trim(),
                input.priceClp(), input.weightGrams(), input.imageKey(), id);
        if (changed != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no existe");
        return product(id);
    }

    @GetMapping("/inventory/movements")
    public java.util.List<Movement> movements(
            @RequestHeader(value = "X-OYL-ADMIN-KEY", required = false) String suppliedKey) {
        authorize(suppliedKey);
        return jdbc.query("""
                SELECT m.*, p.name FROM inventory_movements m JOIN products p ON p.id = m.product_id
                ORDER BY m.created_at DESC, m.id DESC
                """, (rs, i) -> new Movement(rs.getLong("id"), rs.getLong("product_id"),
                    rs.getString("name"), rs.getInt("change_qty"), rs.getInt("before_stock"),
                    rs.getInt("after_stock"), rs.getString("note"), rs.getLong("created_at")));
    }

    @GetMapping("/orders")
    public java.util.List<OrderService.OrderResponse> listOrders(
            @RequestHeader(value = "X-OYL-ADMIN-KEY", required = false) String suppliedKey) {
        authorize(suppliedKey);
        return orders.listAll();
    }

    @PostMapping("/products/{id}/stock")
    @Transactional
    public StockResult adjustStock(
            @PathVariable long id,
            @RequestHeader(value = "X-OYL-ADMIN-KEY", required = false) String suppliedKey,
            @RequestBody StockChange change) {
        authorize(suppliedKey);
        if (change == null || change.delta() == 0 || Math.abs((long) change.delta()) > 10000
                || change.note() == null || change.note().trim().length() < 3
                || change.note().length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movimiento inválido");
        }
        var rows = jdbc.query("SELECT stock FROM products WHERE id = ? FOR UPDATE",
                (rs, index) -> rs.getInt(1), id);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no existe");
        }
        int oldStock = rows.get(0);
        long updated = (long) oldStock + change.delta();
        if (updated < 0 || updated > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock no disponible");
        }
        jdbc.update("UPDATE products SET stock = ? WHERE id = ?", (int) updated, id);
        jdbc.update("""
                INSERT INTO inventory_movements(product_id, change_qty, before_stock,
                                                after_stock, note, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, change.delta(), oldStock, (int) updated, change.note().trim(),
                System.currentTimeMillis());
        return new StockResult(id, oldStock, (int) updated);
    }

    @PostMapping("/orders/{id}/advance")
    @Transactional
    public OrderService.OrderResponse advance(
            @PathVariable String id,
            @RequestHeader(value = "X-OYL-ADMIN-KEY", required = false) String suppliedKey) {
        authorize(suppliedKey);
        var rows = jdbc.query("SELECT status FROM demo_orders WHERE id = ? FOR UPDATE",
                (rs, index) -> rs.getString(1), id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no existe");
        String next = switch (rows.get(0)) {
            case "PENDING" -> "CONFIRMED";
            case "CONFIRMED" -> "PREPARED";
            case "PREPARED" -> "DELIVERED";
            default -> throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pedido no admite más cambios");
        };
        jdbc.update("UPDATE demo_orders SET status = ? WHERE id = ?", next, id);
        jdbc.update("""
                INSERT INTO integration_outbox(event_id, order_id, kind, status, created_at)
                VALUES (?, ?, 'ORDER_STATUS', ?, ?)
                """, "order:" + id + ":" + next.toLowerCase(java.util.Locale.ROOT),
                id, next, System.currentTimeMillis());
        return orders.byId(id);
    }
}
