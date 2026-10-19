package com.oyl.frutossecos.api;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Datos de demostración: se crean solo si todavía no hay productos. */
@Component
public class DemoCatalogSeeder implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public DemoCatalogSeeder(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM products", Long.class);
        if (count != null && count > 0) return;
        Object[][] examples = {
            {1L, "Almendras naturales", "Frutos secos", "Almendras naturales en bolsa de 250 gramos.", 3490, 250, 20, "nut"},
            {2L, "Nueces mariposa", "Frutos secos", "Nueces en mitades en bolsa de 250 gramos.", 3290, 250, 12, "nut"},
            {3L, "Maní tostado", "Frutos secos", "Maní tostado en bolsa de 500 gramos.", 2490, 500, 30, "nut"},
            {4L, "Castañas de cajú", "Frutos secos", "Castañas de cajú de 250 gramos.", 4490, 250, 0, "nut"},
            {5L, "Pasas rubias", "Deshidratados", "Pasas rubias en bolsa de 250 gramos.", 1990, 250, 18, "fruit"},
            {6L, "Cranberries", "Deshidratados", "Cranberries de 250 gramos.", 2790, 250, 8, "fruit"},
            {7L, "Semillas de chía", "Semillas", "Semillas de chía en bolsa de 250 gramos.", 2290, 250, 15, "seed"},
            {8L, "Semillas de zapallo", "Semillas", "Semillas de zapallo de 250 gramos.", 2990, 250, 6, "seed"}
        };
        for (Object[] p : examples) {
            jdbc.update("""
                    INSERT INTO products(id, name, category, description, price_clp,
                            weight_grams, stock, image_key) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, p);
        }
    }
}
