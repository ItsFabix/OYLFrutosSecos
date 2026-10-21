package com.oyl.frutossecos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:oyl_checkout_recovery_test;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class CheckoutRecoveryTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    @BeforeEach void stockForThisIsolatedInMemoryDatabase() {
        jdbc.update("UPDATE products SET stock = 1, price_clp = 3490 WHERE id = 1");
        jdbc.update("UPDATE products SET stock = 0 WHERE id = 4");
    }

    private String request(String id) {
        return """
            {"requestId":"%s","customerAlias":"cliente-recovery",
             "delivery":"DELIVERY","contactPhone":"911111111","contactAddress":"Santiago",
             "items":[{"productId":1,"quantity":1}]}
            """.formatted(id);
    }

    private String send(String json) throws Exception {
        return mvc.perform(post("/api/orders").contentType("application/json").content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    @Test void lostResponseRecoversSameOrderAtZeroStockAndNewPrice() throws Exception {
        String id = UUID.randomUUID().toString();
        String body = request(id);
        String ignoredResponse = send(body); // Client lost this response after commit.
        assertEquals(0, jdbc.queryForObject("SELECT stock FROM products WHERE id = 1", Integer.class));
        jdbc.update("UPDATE products SET price_clp = 9000 WHERE id = 1");
        String recovered = send(body);
        assertEquals(mapper.readTree(ignoredResponse), mapper.readTree(recovered));
        assertEquals(3490, mapper.readTree(recovered).get("totalClp").asInt());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM demo_orders WHERE request_id = ?", Integer.class, id));
        assertEquals(0, jdbc.queryForObject("SELECT stock FROM products WHERE id = 1", Integer.class));
    }

    @Test void concurrentIdenticalRetriesOfLastBagAllReturnSameOrder() throws Exception {
        String id = UUID.randomUUID().toString();
        String body = request(id);
        var pool = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        var ready = new CountDownLatch(8);
        var results = new ArrayList<Future<String>>();
        try {
            for (int i = 0; i < 8; i++) results.add(pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Start timeout");
                return mapper.readTree(send(body)).get("id").asText();
            }));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            String first = results.get(0).get(20, TimeUnit.SECONDS);
            for (var result : results) assertEquals(first, result.get(20, TimeUnit.SECONDS));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM demo_orders WHERE request_id = ?", Integer.class, id));
            assertEquals(0, jdbc.queryForObject("SELECT stock FROM products WHERE id = 1", Integer.class));
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void definitiveStockRejectionIdentifiesRequestAndRollsBackAllLines() throws Exception {
        String id = UUID.randomUUID().toString();
        String json = """
            {"requestId":"%s","customerAlias":"cliente-recovery",
             "items":[{"productId":1,"quantity":1},{"productId":4,"quantity":1}]}
            """.formatted(id);
        mvc.perform(post("/api/orders").contentType("application/json").content(json))
                .andExpect(status().isConflict())
                .andExpect(header().string("X-OYL-Rejected-Request", id))
                .andExpect(jsonPath("$.requestId").value(id));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM demo_orders WHERE request_id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT stock FROM products WHERE id = 1", Integer.class));
    }

    @Test void recoveryUsesDatabaseRatherThanServiceInstanceMemory() throws Exception {
        String id = UUID.randomUUID().toString();
        String first = send(request(id));
        var recreated = new OrderService(jdbc);
        var result = recreated.create(mapper.readValue(request(id), OrderService.CreateOrderRequest.class));
        assertEquals(mapper.readTree(first).get("id").asText(), result.id());
        assertEquals(0, jdbc.queryForObject("SELECT stock FROM products WHERE id = 1", Integer.class));
    }
}
