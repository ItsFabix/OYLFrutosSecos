package com.oyl.frutossecos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:oyl_admin_order_listing_test;DB_CLOSE_DELAY=-1",
    "OYL_ADMIN_KEY=clave-de-prueba-super-segura"
})
@AutoConfigureMockMvc
class AdminOrderListingTest {
    private static final String ADMIN_KEY = "clave-de-prueba-super-segura";
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    void protegeListaDePedidosFiltraYLimita() throws Exception {
        String request = """
            {"requestId":"%s","customerAlias":"cliente-demo",
             "items":[{"productId":2,"quantity":1}]}
            """.formatted(UUID.randomUUID());

        String created = mockMvc.perform(post("/api/orders")
                .contentType("application/json").content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/orders")
                .header("X-OYL-ADMIN-KEY", "clave-incorrecta"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/orders")
                .header("X-OYL-ADMIN-KEY", ADMIN_KEY)
                .param("limit", "1").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].items[0].quantity").value(1));

        mockMvc.perform(get("/api/admin/orders")
                .header("X-OYL-ADMIN-KEY", ADMIN_KEY)
                .param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/admin/orders")
                .header("X-OYL-ADMIN-KEY", ADMIN_KEY)
                .param("limit", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/orders")
                .header("X-OYL-ADMIN-KEY", ADMIN_KEY)
                .param("status", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }
}
