package com.oyl.notifications;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

class NotificationPersistenceTest {
    @TempDir Path directory;
    private static final String TOKEN = "notification-persistence-test-token";
    private ConfigurableApplicationContext start(String url) {
        return new SpringApplicationBuilder(NotificationApplication.class).web(WebApplicationType.NONE).run(
            "--spring.datasource.url=" + url, "--OYL_SERVICE_TOKEN=" + TOKEN,
            "--spring.main.banner-mode=off", "--logging.level.root=ERROR");
    }
    @Test void eventsReadStateAndIdempotencySurviveRestartOfIndependentH2File() {
        String url = "jdbc:h2:file:" + directory.resolve("notifications").toAbsolutePath().toString().replace('\\','/') + ";DB_CLOSE_ON_EXIT=FALSE";
        String order = UUID.randomUUID().toString();
        var input = new NotificationController.EventInput("order:" + order + ":created", order, "ORDER_CREATED", "PENDING", 1234L);
        try (var context = start(url)) {
            var controller = context.getBean(NotificationController.class);
            controller.create(TOKEN, input);
            controller.markRead(input.eventId(), TOKEN);
            assertTrue(controller.list(TOKEN, order).get(0).read());
        }
        try (var context = start(url)) {
            var controller = context.getBean(NotificationController.class);
            var recovered = controller.create(TOKEN, input);
            assertTrue(recovered.read()); assertEquals(input.eventId(), recovered.eventId());
            assertEquals(1, controller.list(TOKEN, order).size());
            assertEquals("PENDING", recovered.status()); assertEquals(1234, recovered.createdAt());
        }
    }
}
