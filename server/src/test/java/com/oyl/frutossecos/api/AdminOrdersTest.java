package com.oyl.frutossecos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
    "spring.datasource.url=jdbc:h2:mem:admin_orders;DB_CLOSE_DELAY=-1",
    "OYL_ADMIN_KEY=admin-orders-test-only-key"
})
@AutoConfigureMockMvc
class AdminOrdersTest {
    private static final String KEY = "admin-orders-test-only-key";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    private String id;

    @BeforeEach void createOrder() throws Exception {
        jdbc.update("DELETE FROM demo_order_lines");
        jdbc.update("DELETE FROM demo_orders");
        jdbc.update("UPDATE products SET stock = 10, price_clp = 3490 WHERE id = 1");
        String body = """
            {"requestId":"%s","customerAlias":"cliente-admin-test",
             "delivery":"DELIVERY","contactPhone":"911111111","contactAddress":"Santiago",
             "items":[{"productId":1,"quantity":2}]}
            """.formatted(UUID.randomUUID());
        String response = mvc.perform(post("/api/orders").contentType("application/json").content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        id = mapper.readTree(response).get("id").asText();
    }

    @Test void listIncludesFullOrderAndRequiresKey() throws Exception {
        mvc.perform(get("/api/admin/orders")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/orders").header("X-OYL-ADMIN-KEY", "wrong"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/orders").header("X-OYL-ADMIN-KEY", KEY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(id))
            .andExpect(jsonPath("$[0].customerAlias").value("cliente-admin-test"))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[0].delivery").value("DELIVERY"))
            .andExpect(jsonPath("$[0].totalClp").value(6980))
            .andExpect(jsonPath("$[0].items[0].productId").value(1))
            .andExpect(jsonPath("$[0].items[0].productName").isNotEmpty())
            .andExpect(jsonPath("$[0].items[0].quantity").value(2));
    }

    @Test void transitionsAreProtectedVisibleToClientAndNeverDiscountStockAgain() throws Exception {
        String path = "/api/admin/orders/" + id + "/advance";
        mvc.perform(post(path)).andExpect(status().isForbidden());
        mvc.perform(post(path).header("X-OYL-ADMIN-KEY", "wrong")).andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/" + id)).andExpect(jsonPath("$.status").value("PENDING"));
        for (String state : new String[]{"CONFIRMED", "PREPARED", "DELIVERED"}) {
            mvc.perform(post(path).header("X-OYL-ADMIN-KEY", KEY)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(state));
            mvc.perform(get("/api/orders/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(state));
            mvc.perform(get("/api/admin/orders").header("X-OYL-ADMIN-KEY", KEY))
                .andExpect(jsonPath("$[0].status").value(state));
            assertEquals(8, jdbc.queryForObject("SELECT stock FROM products WHERE id = 1", Integer.class));
        }
        mvc.perform(post(path).header("X-OYL-ADMIN-KEY", KEY)).andExpect(status().isConflict());
        mvc.perform(post("/api/admin/orders/missing/advance").header("X-OYL-ADMIN-KEY", KEY))
            .andExpect(status().isNotFound());
    }

    @Test void emptyOrUnconfiguredKeyRejectsAccess() {
        for (String configured : new String[]{"", "short"}) {
            var controller = new AdminController(jdbc, new OrderService(jdbc), configured);
            var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.listOrders(configured));
            assertEquals(403, error.getStatusCode().value());
        }
    }
}
