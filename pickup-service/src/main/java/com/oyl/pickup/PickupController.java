package com.oyl.pickup;

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
@RequestMapping("/api/pickup-points")
public class PickupController {
    private final JdbcTemplate jdbc;
    private final String serviceToken;

    public PickupController(JdbcTemplate jdbc, @Value("${OYL_SERVICE_TOKEN:}") String serviceToken) {
        this.jdbc = jdbc;
        this.serviceToken = serviceToken;
    }

    public record Point(String id, String title, String reference, String schedule,
                        double latitude, double longitude) {}
    public record PointInput(String title, String reference, String schedule,
                             Double latitude, Double longitude) {}

    private void authorize(String supplied) {
        if (serviceToken.length() < 16 || supplied == null ||
            !MessageDigest.isEqual(serviceToken.getBytes(StandardCharsets.UTF_8),
                    supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token interno requerido");
        }
    }

    private static void validate(String id, PointInput point) {
        if (id == null || !id.matches("[a-z0-9-]{2,40}") || point == null ||
            point.title() == null || point.title().isBlank() || point.title().length() > 100 ||
            point.reference() == null || point.reference().isBlank() || point.reference().length() > 250 ||
            point.schedule() == null || point.schedule().isBlank() || point.schedule().length() > 160 ||
            point.latitude() == null || !Double.isFinite(point.latitude()) ||
            point.latitude() < -90 || point.latitude() > 90 ||
            point.longitude() == null || !Double.isFinite(point.longitude()) ||
            point.longitude() < -180 || point.longitude() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Punto de retiro inválido");
        }
    }

    private List<Point> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new Point(rs.getString("id"),
                rs.getString("title"), rs.getString("reference"), rs.getString("schedule"),
                rs.getDouble("latitude"), rs.getDouble("longitude")), args);
    }

    @GetMapping
    public List<Point> all() {
        return query("SELECT * FROM pickup_points ORDER BY id");
    }

    @GetMapping("/{id}")
    public Point one(@PathVariable String id) {
        return query("SELECT * FROM pickup_points WHERE id = ?", id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Punto no existe"));
    }

    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Point create(@PathVariable String id,
            @RequestHeader(value = "X-OYL-SERVICE-TOKEN", required = false) String token,
            @RequestBody PointInput point) {
        authorize(token);
        validate(id, point);
        try {
            jdbc.update("INSERT INTO pickup_points VALUES (?, ?, ?, ?, ?, ?)", id,
                    point.title().trim(), point.reference().trim(), point.schedule().trim(),
                    point.latitude(), point.longitude());
        } catch (DuplicateKeyException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Punto ya existe");
        }
        return one(id);
    }

    @PutMapping("/{id}")
    @Transactional
    public Point update(@PathVariable String id,
            @RequestHeader(value = "X-OYL-SERVICE-TOKEN", required = false) String token,
            @RequestBody PointInput point) {
        authorize(token);
        validate(id, point);
        int updated = jdbc.update("""
                UPDATE pickup_points SET title=?, reference=?, schedule=?, latitude=?, longitude=?
                WHERE id=?
                """, point.title().trim(), point.reference().trim(), point.schedule().trim(),
                point.latitude(), point.longitude(), id);
        if (updated != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Punto no existe");
        return one(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable String id,
            @RequestHeader(value = "X-OYL-SERVICE-TOKEN", required = false) String token) {
        authorize(token);
        if (jdbc.update("DELETE FROM pickup_points WHERE id = ?", id) != 1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Punto no existe");
    }

    @GetMapping("/health")
    public Map<String,String> health() {
        return Map.of("status", "ok", "service", "pickup-service");
    }
}
