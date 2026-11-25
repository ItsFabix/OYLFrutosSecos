package com.oyl.notifications;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final JdbcTemplate jdbc;
    private final String serviceToken;

    public NotificationController(JdbcTemplate jdbc,
            @Value("${OYL_SERVICE_TOKEN:}") String serviceToken) {
        this.jdbc = jdbc;
        this.serviceToken = serviceToken;
    }

    public record Event(String eventId, String orderId, String kind,
                        String status, long createdAt, boolean read) {}
    public record EventInput(String eventId, String orderId, String kind, String status, Long createdAt) {}

    private void authorize(String supplied) {
        if (serviceToken.length() < 16 || supplied == null ||
                !MessageDigest.isEqual(serviceToken.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token interno requerido");
        }
    }

    private static void validate(EventInput event) {
        if (event == null || event.eventId() == null ||
                !event.eventId().matches("[A-Za-z0-9:_-]{4,120}") ||
                event.orderId() == null || !event.orderId().matches("[a-fA-F0-9-]{36}") ||
                (event.kind() == null || !List.of("ORDER_CREATED", "ORDER_STATUS").contains(event.kind())) ||
                (event.status() == null || !List.of("PENDING", "CONFIRMED", "PREPARED", "DELIVERED").contains(event.status())) ||
                event.createdAt() == null || event.createdAt() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento inválido");
        }
    }

    private List<Event> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new Event(rs.getString("event_id"),
            rs.getString("order_id"), rs.getString("kind"), rs.getString("status"),
            rs.getLong("created_at"), rs.getBoolean("read_flag")), args);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "notification-service");
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Event create(@RequestHeader(value = "X-OYL-SERVICE-TOKEN", required = false) String token,
                        @RequestBody EventInput input) {
        authorize(token);
        validate(input);
        try {
            jdbc.update("""
                INSERT INTO notification_events(event_id, order_id, kind, status, created_at, read_flag)
                VALUES (?, ?, ?, ?, ?, FALSE)
                """, input.eventId(), input.orderId(), input.kind(), input.status(), input.createdAt());
        } catch (DuplicateKeyException duplicate) {
            Event saved = oneInternal(input.eventId());
            if (!saved.orderId().equals(input.orderId()) || !saved.kind().equals(input.kind()) ||
                    !saved.status().equals(input.status()) || saved.createdAt() != input.createdAt())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "eventId ya utilizado");
            return saved;
        }
        return oneInternal(input.eventId());
    }

    private Event oneInternal(String eventId) {
        return query("SELECT * FROM notification_events WHERE event_id=?", eventId).stream()
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no existe"));
    }

    @GetMapping("/events")
    public List<Event> list(@RequestHeader(value = "X-OYL-SERVICE-TOKEN", required = false) String token,
                            @RequestParam String orderId) {
        authorize(token);
        if (orderId == null || !orderId.matches("[a-fA-F0-9-]{36}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId inválido");
        return query("SELECT * FROM notification_events WHERE order_id=? ORDER BY created_at,event_id", orderId);
    }

    @PutMapping("/events/{eventId}/read")
    public Event markRead(@PathVariable String eventId,
            @RequestHeader(value = "X-OYL-SERVICE-TOKEN", required = false) String token) {
        authorize(token);
        if (jdbc.update("UPDATE notification_events SET read_flag=TRUE WHERE event_id=?", eventId) != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no existe");
        return oneInternal(eventId);
    }

    @DeleteMapping("/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String eventId,
            @RequestHeader(value = "X-OYL-SERVICE-TOKEN", required = false) String token) {
        authorize(token);
        if (jdbc.update("DELETE FROM notification_events WHERE event_id=?", eventId) != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no existe");
    }
}
