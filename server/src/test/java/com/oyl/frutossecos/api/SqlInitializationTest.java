package com.oyl.frutossecos.api;

import com.oyl.frutossecos.OylApiApplication;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;

/** Arranque HTTP real, archivo nuevo de prueba y segundo arranque sobre ese mismo archivo. */
class SqlInitializationTest {
    @TempDir Path directory;

    private ServletWebServerApplicationContext start(String url) {
        return (ServletWebServerApplicationContext) SpringApplication.run(OylApiApplication.class,
                "--spring.datasource.url=" + url, "--server.port=0", "--server.address=127.0.0.1",
                "--spring.main.banner-mode=off", "--OYL_ADMIN_KEY=", "--OYL_SERVICE_TOKEN=");
    }

    @Test void emptyDatabaseLoadsSqlAndRestartPreservesOrdersStockEditsAndAudit() {
        String url = "jdbc:h2:file:" + directory.resolve("isolated-control3").toAbsolutePath()
                .toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE";
        String requestId = UUID.randomUUID().toString();
        String orderId;
        try (var context = start(url)) {
            var jdbc = context.getBean(JdbcTemplate.class);
            assertFalse(context.containsBean("demoCatalogSeeder"));
            assertEquals(8, jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class));
            var http = RestClient.create("http://127.0.0.1:" + context.getWebServer().getPort());
            var products = http.get().uri("/api/products").retrieve().body(ProductController.ProductDto[].class);
            assertNotNull(products);
            assertEquals(8, products.length);
            assertEquals("Semillas de chía", products[6].name());
            assertTrue(http.get().uri("/api/health").retrieve().body(String.class).contains("ok"));
            http.get().uri("/api/admin/orders").exchange((req, response) -> {
                assertEquals(403, response.getStatusCode().value()); return null;
            });
            jdbc.update("UPDATE products SET stock = 3, price_clp = 4000, name = 'Almendras editadas' WHERE id = 1");
            var orders = context.getBean(OrderService.class);
            orderId = orders.create(new OrderService.CreateOrderRequest(requestId, "cliente-sql",
                    List.of(new OrderService.LineRequest(1, 1)), "PICKUP", "", "")).id();
            assertEquals(2, jdbc.queryForObject("SELECT stock FROM products WHERE id = 1", Integer.class));
        }
        try (var context = start(url)) {
            var jdbc = context.getBean(JdbcTemplate.class);
            assertEquals(8, jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class));
            assertEquals("Almendras editadas", jdbc.queryForObject("SELECT name FROM products WHERE id = 1", String.class));
            assertEquals(4000, jdbc.queryForObject("SELECT price_clp FROM products WHERE id = 1", Integer.class));
            assertEquals(2, jdbc.queryForObject("SELECT stock FROM products WHERE id = 1", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM demo_orders", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM demo_order_lines", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM inventory_movements", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM integration_outbox WHERE delivered_at IS NULL", Integer.class));
            var recovered = context.getBean(OrderService.class).create(new OrderService.CreateOrderRequest(
                requestId, "cliente-sql", List.of(new OrderService.LineRequest(1, 1)), "PICKUP", "", ""));
            assertEquals(orderId, recovered.id());
            assertEquals(2, jdbc.queryForObject("SELECT stock FROM products WHERE id = 1", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM integration_outbox", Integer.class));
        }
    }
}
