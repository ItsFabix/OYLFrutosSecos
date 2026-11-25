package com.oyl.frutossecos.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final JdbcTemplate jdbc;

    public OrderService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record LineRequest(long productId, int quantity) {}
    public record CreateOrderRequest(String requestId, String customerAlias, List<LineRequest> items,
                                     String delivery, String contactPhone, String contactAddress) {}
    public record OrderLine(long productId, String productName, int quantity, int unitPriceClp) {}
    public record OrderResponse(String id, String requestId, String customerAlias,
                                String status, long totalClp, List<OrderLine> items,
                                String delivery, String contactPhone, String contactAddress,
                                long createdAt) {}
    private record LockedProduct(long id, String name, int price, int stock) {}

    /** Only emitted while holding the request's product lock, before any order can commit. */
    public static class StockRejectedException extends RuntimeException {
        private final String requestId;
        StockRejectedException(String requestId, String message) {
            super(message);
            this.requestId = requestId;
        }
        public String requestId() { return requestId; }
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest req) {
        if (req == null || req.requestId() == null || req.customerAlias() == null
                || req.customerAlias().trim().length() < 2 || req.customerAlias().length() > 80
                || req.items() == null || req.items().isEmpty() || req.items().size() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de pedido inválidos");
        }
        final String requestId;
        try {
            requestId = UUID.fromString(req.requestId()).toString();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestId debe ser UUID");
        }
        String delivery = req.delivery() == null ? "PICKUP" : req.delivery();
        String phone = req.contactPhone() == null ? "" : req.contactPhone().trim();
        String address = req.contactAddress() == null ? "" : req.contactAddress().trim();
        if ((!delivery.equals("PICKUP") && !delivery.equals("DELIVERY"))
                || phone.length() > 80 || address.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de entrega inválidos");
        }
        // H2 MERGE bloquea una sola fila por requestId hasta commit/rollback.
        // La unicidad no depende de que los reintentos compartan productos.
        jdbc.update("MERGE INTO checkout_requests(request_id) KEY(request_id) VALUES (?)", requestId);
        var found = jdbc.query("SELECT id FROM demo_orders WHERE request_id = ?",
                (rs, i) -> rs.getString(1), requestId);
        if (!found.isEmpty()) {
            var previous = byId(found.get(0));
            var previousItems = previous.items().stream()
                    .map(line -> new LineRequest(line.productId(), line.quantity()))
                    .sorted(java.util.Comparator.comparingLong(LineRequest::productId)).toList();
            if (req.items().stream().anyMatch(java.util.Objects::isNull)
                    || !previous.customerAlias().equals(req.customerAlias().trim())
                    || !previous.delivery().equals(delivery)
                    || !previous.contactPhone().equals(phone)
                    || !previous.contactAddress().equals(address)
                    || !previousItems.equals(req.items().stream()
                        .sorted(java.util.Comparator.comparingLong(LineRequest::productId)).toList())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "requestId ya utilizado para una solicitud diferente");
            }
            return previous;
        }

        var unique = new HashSet<Long>();
        for (var item : req.items()) {
            if (item == null || item.productId() <= 0 || item.quantity() <= 0
                    || item.quantity() > 100 || !unique.add(item.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Productos o cantidades inválidas");
            }
        }

        // Los bloqueos se adquieren por ID ascendente: evita descontar stock simultáneamente.
        var sorted = req.items().stream()
                .sorted(java.util.Comparator.comparingLong(LineRequest::productId)).toList();
        var lines = new ArrayList<OrderLine>();
        long total = 0;
        for (var requested : sorted) {
            var locked = jdbc.query("""
                    SELECT id, name, price_clp, stock FROM products WHERE id = ? FOR UPDATE
                    """, (rs, i) -> new LockedProduct(
                        rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getInt(4)),
                    requested.productId());
            if (locked.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el producto " + requested.productId());
            }
            var product = locked.get(0);
            if (lines.isEmpty()) {
                // Identical retries share their first product lock. The initial lookup may
                // have run before another transaction committed; check again UNDER the lock.
                // This must precede the stock check, including when the first order used the last bag.
                var committed = jdbc.query("SELECT id FROM demo_orders WHERE request_id = ?",
                        (rs, i) -> rs.getString(1), requestId);
                if (!committed.isEmpty()) return byId(committed.get(0));
            }
            if (requested.quantity() > product.stock()) {
                throw new StockRejectedException(requestId,
                        "Stock insuficiente para " + product.name());
            }
            try {
                total = Math.addExact(total,
                        Math.multiplyExact((long) product.price(), requested.quantity()));
            } catch (ArithmeticException overflow) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total fuera de rango");
            }
            lines.add(new OrderLine(product.id(), product.name(),
                    requested.quantity(), product.price()));
        }
        String orderId = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO demo_orders(id, request_id, customer_alias, created_at, status,
                                        total_clp, delivery, contact_phone, contact_address)
                VALUES(?, ?, ?, ?, 'PENDING', ?, ?, ?, ?)
                """, orderId, requestId, req.customerAlias().trim(), System.currentTimeMillis(),
                total, delivery, phone, address);
        for (var line : lines) {
            jdbc.update("""
                    INSERT INTO demo_order_lines(order_id, product_id, product_name, quantity, unit_price_clp)
                    VALUES (?, ?, ?, ?, ?)
                    """, orderId, line.productId(), line.productName(),
                    line.quantity(), line.unitPriceClp());
            int count = jdbc.update(
                    "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?",
                    line.quantity(), line.productId(), line.quantity());
            if (count != 1) {
                throw new StockRejectedException(requestId,
                        "El stock cambió; vuelve a intentar");
            }
            int afterStock = jdbc.queryForObject("SELECT stock FROM products WHERE id = ?",
                    Integer.class, line.productId());
            jdbc.update("""
                    INSERT INTO inventory_movements(product_id, change_qty, before_stock,
                        after_stock, note, created_at) VALUES (?, ?, ?, ?, ?, ?)
                    """, line.productId(), -line.quantity(), afterStock + line.quantity(),
                    afterStock, "Pedido REST " + orderId, System.currentTimeMillis());
        }
        // Outbox en la MISMA transacción: un pedido confirmado siempre deja un evento
        // pendiente de envío; la venta NO depende de la disponibilidad del otro servicio.
        jdbc.update("""
                INSERT INTO integration_outbox(event_id, order_id, kind, status, created_at)
                VALUES (?, ?, 'ORDER_CREATED', 'PENDING', ?)
                """, "order:" + orderId + ":created", orderId, System.currentTimeMillis());
        return byId(orderId);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listAll() {
        return jdbc.query("SELECT id FROM demo_orders ORDER BY created_at DESC, id DESC",
                (rs, i) -> rs.getString(1)).stream().map(this::byId).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse byId(String id) {
        var rows = jdbc.query("""
                SELECT id, request_id, customer_alias, status, total_clp, delivery,
                       contact_phone, contact_address, created_at
                FROM demo_orders WHERE id = ?
                """, (rs, i) -> new String[] {
                    rs.getString("id"), rs.getString("request_id"),
                    rs.getString("customer_alias"), rs.getString("status"),
                    rs.getString("total_clp"), rs.getString("delivery"),
                    rs.getString("contact_phone"), rs.getString("contact_address"),
                    rs.getString("created_at") }, id);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado");
        }
        var row = rows.get(0);
        var lines = jdbc.query("""
                SELECT product_id, product_name, quantity, unit_price_clp
                FROM demo_order_lines WHERE order_id = ? ORDER BY product_id
                """, (rs, i) -> new OrderLine(
                    rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getInt(4)), id);
        return new OrderResponse(row[0], row[1], row[2], row[3],
                Long.parseLong(row[4]), lines, row[5], row[6], row[7], Long.parseLong(row[8]));
    }
}
