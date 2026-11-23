package com.oyl.frutossecos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:control3;DB_CLOSE_DELAY=-1",
    "OYL_ADMIN_KEY=control3-test-only-admin-key"
})
@AutoConfigureMockMvc
class Control3IntegrationTest {
    private static final String KEY = "control3-test-only-admin-key";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private String productJson(int stock) {
        return """
            {"name":"Producto de integración","category":"Semillas","description":"Bolsa para pruebas",
             "priceClp":3000,"weightGrams":250,"stock":%d,"imageKey":"seed"}
            """.formatted(stock);
    }

    private long createProduct(int stock) throws Exception {
        var body = mvc.perform(post("/api/admin/products").header("X-OYL-ADMIN-KEY", KEY)
                .contentType("application/json").content(productJson(stock)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }

    private String request(String request, long product, int qty) {
        return """
            {"requestId":"%s","customerAlias":"cliente-control3","delivery":"DELIVERY",
             "contactPhone":"912345678","contactAddress":"Santiago","items":[{"productId":%d,"quantity":%d}]}
            """.formatted(request, product, qty);
    }

    private String order(String body) throws Exception {
        return mapper.readTree(mvc.perform(post("/api/orders").contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    @Test void customerAdminCustomerStatusCycleAndFinalStateConflict() throws Exception {
        long product = createProduct(5);
        String id = order(request(UUID.randomUUID().toString(), product, 2));
        assertEquals(3, jdbc.queryForObject("SELECT stock FROM products WHERE id = ?", Integer.class, product));
        mvc.perform(get("/api/admin/orders").header("X-OYL-ADMIN-KEY", KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '" + id + "')].status").value("PENDING"));
        for (String state : new String[] {"CONFIRMED", "PREPARED", "DELIVERED"}) {
            mvc.perform(post("/api/admin/orders/" + id + "/advance").header("X-OYL-ADMIN-KEY", KEY))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state));
            mvc.perform(get("/api/orders/" + id)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(state))
                    .andExpect(jsonPath("$.contactAddress").value("Santiago"))
                    .andExpect(jsonPath("$.createdAt").isNumber());
        }
        mvc.perform(post("/api/admin/orders/" + id + "/advance").header("X-OYL-ADMIN-KEY", KEY))
                .andExpect(status().isConflict());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM inventory_movements WHERE product_id = ? AND change_qty = -2",
                Integer.class, product));
    }

    @Test void allAdministrativeEndpointsRejectMissingAndIncorrectKeys() throws Exception {
        for (String key : new String[] {"", "incorrecta"}) {
            mvc.perform(get("/api/admin/orders").header("X-OYL-ADMIN-KEY", key)).andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/inventory/movements").header("X-OYL-ADMIN-KEY", key)).andExpect(status().isForbidden());
            mvc.perform(post("/api/admin/products").header("X-OYL-ADMIN-KEY", key)
                    .contentType("application/json").content(productJson(1))).andExpect(status().isForbidden());
            mvc.perform(put("/api/admin/products/1").header("X-OYL-ADMIN-KEY", key)
                    .contentType("application/json").content(productJson(999))).andExpect(status().isForbidden());
            mvc.perform(post("/api/admin/products/1/stock").header("X-OYL-ADMIN-KEY", key)
                    .contentType("application/json").content("{\"delta\":1,\"note\":\"prueba\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/admin/orders/no-existe/advance").header("X-OYL-ADMIN-KEY", key))
                    .andExpect(status().isForbidden());
        }
    }

    @Test void editingNeverOverwritesStockAndMovementsAreValidatedAndListed() throws Exception {
        long id = createProduct(7);
        mvc.perform(put("/api/admin/products/" + id).header("X-OYL-ADMIN-KEY", KEY)
                .contentType("application/json").content(productJson(999)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(7));
        mvc.perform(post("/api/admin/products/" + id + "/stock").header("X-OYL-ADMIN-KEY", KEY)
                .contentType("application/json").content("{\"delta\":-8,\"note\":\"Sin existencia\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/admin/products/" + id + "/stock").header("X-OYL-ADMIN-KEY", KEY)
                .contentType("application/json").content("{\"delta\":-2,\"note\":\"Salida de prueba\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.newStock").value(5));
        mvc.perform(get("/api/admin/inventory/movements").header("X-OYL-ADMIN-KEY", KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.productId == " + id + " && @.delta == -2)].afterStock").value(5));
    }

    @Test void invalidProductAndMissingCategoryReturn400InsteadOf500() throws Exception {
        for (String body : new String[] {"{}", productJson(-1), productJson(1).replace("\"category\":\"Semillas\",", ""),
                productJson(1).replace("\"seed\"", "\"https://external/photo.jpg\"")}) {
            mvc.perform(post("/api/admin/products").header("X-OYL-ADMIN-KEY", KEY)
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
    }

    @Test void sameRequestWithDifferentPayloadIsRejectedWithoutAnotherStockDiscount() throws Exception {
        long id = createProduct(10);
        String request = UUID.randomUUID().toString();
        order(request(request, id, 1));
        mvc.perform(post("/api/orders").contentType("application/json").content(request(request, id, 2)))
                .andExpect(status().isConflict()).andExpect(header().doesNotExist("X-OYL-Rejected-Request"));
        assertEquals(9, jdbc.queryForObject("SELECT stock FROM products WHERE id = ?", Integer.class, id));
    }

    @Test void concurrentDifferentRequestsCannotOversellLastBag() throws Exception {
        long id = createProduct(1);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> place = () -> {
                start.await(10, TimeUnit.SECONDS);
                return mvc.perform(post("/api/orders").contentType("application/json")
                        .content(request(UUID.randomUUID().toString(), id, 1))).andReturn().getResponse().getStatus();
            };
            var a = pool.submit(place);
            var b = pool.submit(place);
            start.countDown();
            assertEquals(java.util.Set.of(201, 409), java.util.Set.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)));
            assertEquals(0, jdbc.queryForObject("SELECT stock FROM products WHERE id = ?", Integer.class, id));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM demo_order_lines WHERE product_id = ?", Integer.class, id));
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void concurrentChangedPayloadWithSameRequestHasOneWinnerAndOneConflict() throws Exception {
        long a = createProduct(1), b = createProduct(1);
        String request = UUID.randomUUID().toString();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> { start.await(); return mvc.perform(post("/api/orders")
                .contentType("application/json").content(request(request, a, 1))).andReturn().getResponse().getStatus(); });
            var second = pool.submit(() -> { start.await(); return mvc.perform(post("/api/orders")
                .contentType("application/json").content(request(request, b, 1))).andReturn().getResponse().getStatus(); });
            start.countDown();
            assertEquals(java.util.Set.of(201, 409), java.util.Set.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM demo_orders WHERE request_id = ?", Integer.class, request));
        } finally { start.countDown(); pool.shutdownNow(); }
    }
}
