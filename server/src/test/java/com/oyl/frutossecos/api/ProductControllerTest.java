package com.oyl.frutossecos.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:oyl_server_test;DB_CLOSE_DELAY=-1",
    "OYL_ADMIN_KEY=clave-de-prueba-super-segura"
})
@AutoConfigureMockMvc
class ProductControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    @Test
    void saludYCatalogoResponden() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].name").value("Almendras naturales"));
    }

    @Test
    void pedidoDescuentaStockEsIdempotenteYRechazaSobreventa() throws Exception {
        int stockInicial = jdbc.queryForObject(
                "SELECT stock FROM products WHERE id = 1", Integer.class);
        String key = UUID.randomUUID().toString();
        String json = """
            {"requestId":"%s","customerAlias":"cliente-demo",
             "delivery":"DELIVERY","contactPhone":"+56 9 1111 1111",
             "contactAddress":"Santiago de prueba",
             "items":[{"productId":1,"quantity":2}]}
            """.formatted(key);
        String body = mockMvc.perform(post("/api/orders")
                .contentType("application/json").content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalClp").value(6980))
                .andExpect(jsonPath("$.delivery").value("DELIVERY"))
                .andExpect(jsonPath("$.contactAddress").value("Santiago de prueba"))
                .andReturn().getResponse().getContentAsString();
        JsonNode order = mapper.readTree(body);
        String orderId = order.get("id").asText();
        int stockFinal = jdbc.queryForObject(
                "SELECT stock FROM products WHERE id = 1", Integer.class);
        assertEquals(stockInicial - 2, stockFinal);

        mockMvc.perform(post("/api/orders")
                .contentType("application/json").content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId));
        assertEquals(stockFinal, jdbc.queryForObject(
                "SELECT stock FROM products WHERE id = 1", Integer.class));

        String noStock = """
            {"requestId":"%s","customerAlias":"cliente-demo",
             "items":[{"productId":1,"quantity":100}]}
            """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/orders").contentType("application/json")
                .content(noStock)).andExpect(status().isConflict());
        assertEquals(stockFinal, jdbc.queryForObject(
                "SELECT stock FROM products WHERE id = 1", Integer.class));
    }

    @Test
    void adminRequiereClaveYRegistraMovimientos() throws Exception {
        mockMvc.perform(post("/api/admin/products/2/stock")
                .contentType("application/json")
                .content("{\"delta\":2,\"note\":\"Ingreso de prueba\"}"))
                .andExpect(status().isForbidden());
        int stockAntes = jdbc.queryForObject(
                "SELECT stock FROM products WHERE id = 2", Integer.class);
        mockMvc.perform(post("/api/admin/products/2/stock")
                .header("X-OYL-ADMIN-KEY", "clave-de-prueba-super-segura")
                .contentType("application/json")
                .content("{\"delta\":2,\"note\":\"Ingreso de prueba\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStock").value(stockAntes + 2));
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_movements WHERE product_id = 2",
                Integer.class);
        assertTrue(count != null && count >= 1);
    }
}
