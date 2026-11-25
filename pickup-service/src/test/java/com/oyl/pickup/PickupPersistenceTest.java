package com.oyl.pickup;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

class PickupPersistenceTest {
    @TempDir Path directory;
    private static final String TOKEN = "pickup-persistence-test-token";
    private ConfigurableApplicationContext start(String url) {
        return new SpringApplicationBuilder(PickupApplication.class).web(WebApplicationType.NONE).run(
            "--spring.datasource.url=" + url, "--OYL_SERVICE_TOKEN=" + TOKEN,
            "--spring.main.banner-mode=off", "--logging.level.root=ERROR");
    }
    @Test void independentFileDatabasePreservesChangesAndDoesNotDuplicateSqlSeedsAfterRestart() {
        String url = "jdbc:h2:file:" + directory.resolve("pickup").toAbsolutePath().toString().replace('\\','/') + ";DB_CLOSE_ON_EXIT=FALSE";
        try (var context = start(url)) {
            var controller = context.getBean(PickupController.class);
            assertEquals(2, controller.all().size());
            controller.update("nunoa", TOKEN, new PickupController.PointInput("Ñuñoa editado", "Referencia de prueba", "L-V 10-17", -33.4554, -70.5982));
            controller.create("test", TOKEN, new PickupController.PointInput("Retiro aislado", "Dirección de prueba", "L-V 10-17", -33.4, -70.6));
        }
        try (var context = start(url)) {
            var controller = context.getBean(PickupController.class);
            assertEquals(3, controller.all().size());
            assertEquals("Ñuñoa editado", controller.one("nunoa").title());
            assertEquals(-33.4554, controller.one("nunoa").latitude());
            assertEquals("Retiro aislado", controller.one("test").title());
            assertEquals(3, context.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM pickup_points", Integer.class));
        }
    }
}
