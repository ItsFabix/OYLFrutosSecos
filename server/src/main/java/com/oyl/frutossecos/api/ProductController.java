package com.oyl.frutossecos.api;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProductController {
    private final JdbcTemplate jdbc;

    public ProductController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ProductDto(long id, String name, String category,
                             String description, int priceClp, int weightGrams,
                             int stock, String imageKey) {}

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "OYL REST API", "mode", "demo");
    }

    @GetMapping("/products")
    public List<ProductDto> products() {
        return jdbc.query("""
                SELECT id, name, category, description, price_clp,
                       weight_grams, stock, image_key
                FROM products ORDER BY id
                """,
                (rs, row) -> new ProductDto(
                    rs.getLong("id"), rs.getString("name"), rs.getString("category"),
                    rs.getString("description"), rs.getInt("price_clp"),
                    rs.getInt("weight_grams"), rs.getInt("stock"),
                    rs.getString("image_key")
                ));
    }
}
