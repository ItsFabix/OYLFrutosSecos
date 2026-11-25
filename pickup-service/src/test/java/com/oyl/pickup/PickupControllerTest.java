package com.oyl.pickup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:pickup_test;DB_CLOSE_DELAY=-1",
    "OYL_SERVICE_TOKEN=token-interno-para-pruebas"
})
@AutoConfigureMockMvc
class PickupControllerTest {
    @Autowired MockMvc mvc;
    private static final String TOKEN = "token-interno-para-pruebas";
    private static final String VALID = """
        {"title":"Punto Test","reference":"Dirección de prueba","schedule":"L-V 10-18",
         "latitude":-33.4,"longitude":-70.6}
        """;

    @Test void seedAndPublicRead() throws Exception {
        mvc.perform(get("/api/pickup-points")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/pickup-points/centro")).andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Punto Centro (demo)"));
    }

    @Test void fullCrudAndAuth() throws Exception {
        mvc.perform(post("/api/pickup-points/test").contentType("application/json")
            .content(VALID)).andExpect(status().isForbidden());
        mvc.perform(post("/api/pickup-points/test").header("X-OYL-SERVICE-TOKEN", TOKEN)
            .contentType("application/json").content(VALID)).andExpect(status().isCreated());
        mvc.perform(post("/api/pickup-points/test").header("X-OYL-SERVICE-TOKEN", TOKEN)
            .contentType("application/json").content(VALID)).andExpect(status().isConflict());
        mvc.perform(put("/api/pickup-points/test").header("X-OYL-SERVICE-TOKEN", TOKEN)
            .contentType("application/json").content(VALID.replace("Punto Test", "Punto editado")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Punto editado"));
        mvc.perform(delete("/api/pickup-points/test").header("X-OYL-SERVICE-TOKEN", TOKEN))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/pickup-points/test")).andExpect(status().isNotFound());
    }

    @Test void rejectsInvalidLocation() throws Exception {
        mvc.perform(post("/api/pickup-points/invalid").header("X-OYL-SERVICE-TOKEN", TOKEN)
            .contentType("application/json").content(VALID.replace("-33.4", "999")))
            .andExpect(status().isBadRequest());
    }
}
