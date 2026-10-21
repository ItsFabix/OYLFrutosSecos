package com.oyl.frutossecos.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
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

    private void authorize(String candidate) {
        if (key == null || key.length() < 16 || candidate == null
                || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),
                        candidate.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Clave de administrador no configurada o incorrecta");
        }
    }

    /**
     * Lista paginada (hasta 100) de pedidos almacenados en H2.
     * Solo accesible con la clave administrativa configurada en el servidor.
     */
    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public java.util.List<OrderService.OrderResponse> listOrders(
            @RequestHeader(value = "X-OYL-ADMIN-KEY", required = false) String suppliedKey,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status) {
        authorize(suppliedKey);
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El limite debe estar entre 1 y 100");
        }
        if (status != null && !java.util.Set.of(
                "PENDING", "CONFIRMED", "PREPARED", "DELIVERED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no valido");
        }

        java.util.List<String> ids;
        if (status == null) {
            ids = jdbc.query(
                    "SELECT id FROM demo_orders ORDER BY created_at DESC, id DESC LIMIT ?",
                    (rs, index) -> rs.getString("id"), limit);
        } else {
            ids = jdbc.query(
                    "SELECT id FROM demo_orders WHERE status = ? " +
                    "ORDER BY created_at DESC, id DESC LIMIT ?",
                    (rs, index) -> rs.getString("id"), status, limit);
        }
        return ids.stream().map(orders::byId).toList();
    }

    public record StockChange(int delta, String note) {}
    public record StockResult(long productId, int oldStock, int newStock) {}

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
        return orders.byId(id);
    }
}
