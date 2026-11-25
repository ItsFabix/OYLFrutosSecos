package com.oyl.frutossecos.api;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:oyl_outbox_test;DB_CLOSE_DELAY=-1",
    "OYL_ADMIN_KEY=clave-para-pruebas-admin"
})
@AutoConfigureMockMvc
class OutboxIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test void orderAndStatusCreateAtomicPendingEventsWithoutBreakingCheckout() throws Exception {
        String requestId=UUID.randomUUID().toString();
        String json="{\"requestId\":\""+requestId+"\",\"customerAlias\":\"usuario-demo\",\"items\":[{\"productId\":1,\"quantity\":1}]}";
        String result=mvc.perform(post("/api/orders").contentType("application/json")
            .content(json)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String orderId=new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(result).get("id").asText();
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM integration_outbox WHERE order_id=?",Integer.class,orderId));
        mvc.perform(post("/api/orders").contentType("application/json")
            .content(json)).andExpect(status().isCreated());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM integration_outbox WHERE order_id=?",Integer.class,orderId));
        mvc.perform(post("/api/admin/orders/"+orderId+"/advance")
            .header("X-OYL-ADMIN-KEY","clave-para-pruebas-admin"))
            .andExpect(status().isOk());
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM integration_outbox WHERE order_id=?",Integer.class,orderId));
    }
    @Test void checkoutContinuesWithoutSideServices() throws Exception {
        mvc.perform(get("/api/pickup-points")).andExpect(status().isServiceUnavailable());
        mvc.perform(post("/api/orders").contentType("application/json").content("""
            {"requestId":"00000000-0000-4000-8000-000000000007","customerAlias":"cliente-prueba",
             "items":[{"productId":2,"quantity":1}]}
            """)).andExpect(status().isCreated());
    }
}
