package com.oyl.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification_test;DB_CLOSE_DELAY=-1",
    "OYL_SERVICE_TOKEN=token-interno-para-pruebas"
})
@AutoConfigureMockMvc
class NotificationControllerTest {
    @Autowired MockMvc mvc;
    static final String TOKEN = "token-interno-para-pruebas";
    static final String ORDER = "123e4567-e89b-12d3-a456-426614174000";
    static final String EVENT = """
        {"eventId":"order:123e4567-e89b-12d3-a456-426614174000:created",
        "orderId":"123e4567-e89b-12d3-a456-426614174000",
        "kind":"ORDER_CREATED","status":"PENDING","createdAt":1730000000000}
        """;
    @Test void authorizationAndIdempotency() throws Exception {
        mvc.perform(post("/api/notifications/events").contentType("application/json")
                .content(EVENT)).andExpect(status().isForbidden());
        for (int i=0;i<2;i++) mvc.perform(post("/api/notifications/events")
            .header("X-OYL-SERVICE-TOKEN",TOKEN).contentType("application/json").content(EVENT))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/notifications/events").param("orderId",ORDER)
            .header("X-OYL-SERVICE-TOKEN",TOKEN)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }
    @Test void crudAndPayloadConflict() throws Exception {
        String id = "order:123e4567-e89b-12d3-a456-426614174001:created";
        String body = EVENT.replace("426614174000:created", "426614174001:created");
        mvc.perform(post("/api/notifications/events").header("X-OYL-SERVICE-TOKEN",TOKEN)
            .contentType("application/json").content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/notifications/events").header("X-OYL-SERVICE-TOKEN",TOKEN)
            .contentType("application/json").content(body.replace("PENDING", "DELIVERED")))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/notifications/events/"+id+"/read")
            .header("X-OYL-SERVICE-TOKEN",TOKEN))
            .andExpect(status().isOk()).andExpect(jsonPath("$.read").value(true));
        mvc.perform(delete("/api/notifications/events/"+id)
            .header("X-OYL-SERVICE-TOKEN",TOKEN)).andExpect(status().isNoContent());
    }
    @Test void invalidInput() throws Exception {
        mvc.perform(post("/api/notifications/events").header("X-OYL-SERVICE-TOKEN",TOKEN)
            .contentType("application/json").content(EVENT.replace("ORDER_CREATED","NOT_VALID")))
            .andExpect(status().isBadRequest());
    }
}
