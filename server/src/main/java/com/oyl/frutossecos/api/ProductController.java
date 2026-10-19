package com.oyl.frutossecos.api;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Paso 11A: API REST de lectura, con datos ficticios.
 * En la siguiente etapa los productos y pedidos se persistirán en el servidor.
 */
@RestController
@RequestMapping("/api")
public class ProductController {
    public record ProductDto(
            long id, String name, String category, String description,
            int priceClp, int weightGrams, int stock, String imageKey) {}

    private static final List<ProductDto> PRODUCTS = List.of(
            new ProductDto(1, "Almendras naturales", "Frutos secos",
                    "Almendras naturales en bolsa de 250 gramos.", 3490, 250, 20, "nut"),
            new ProductDto(2, "Nueces mariposa", "Frutos secos",
                    "Nueces en mitades en bolsa de 250 gramos.", 3290, 250, 12, "nut"),
            new ProductDto(3, "Maní tostado", "Frutos secos",
                    "Maní tostado en bolsa de 500 gramos.", 2490, 500, 30, "nut"),
            new ProductDto(4, "Castañas de cajú", "Frutos secos",
                    "Castañas de cajú en presentación de 250 gramos.", 4490, 250, 0, "nut"),
            new ProductDto(5, "Pasas rubias", "Deshidratados",
                    "Pasas rubias en bolsa de 250 gramos.", 1990, 250, 18, "fruit"),
            new ProductDto(6, "Cranberries", "Deshidratados",
                    "Cranberries deshidratados de 250 gramos.", 2790, 250, 8, "fruit"),
            new ProductDto(7, "Semillas de chía", "Semillas",
                    "Semillas de chía en bolsa de 250 gramos.", 2290, 250, 15, "seed"),
            new ProductDto(8, "Semillas de zapallo", "Semillas",
                    "Semillas de zapallo en bolsa de 250 gramos.", 2990, 250, 6, "seed")
    );

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "OYL REST API", "mode", "demo");
    }

    @GetMapping("/products")
    public List<ProductDto> products() {
        return PRODUCTS;
    }
}
