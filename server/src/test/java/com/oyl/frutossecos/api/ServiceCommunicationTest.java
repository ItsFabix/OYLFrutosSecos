package com.oyl.frutossecos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real loopback HTTP to an isolated recipient; never uses the running services or their H2 files. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:service_communication_test;DB_CLOSE_DELAY=-1",
    "OYL_SERVICE_TOKEN=", "OYL_ADMIN_KEY="
})
class ServiceCommunicationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired OrderService orders;
    private HttpServer recipient;
    private String url;
    private static final String TOKEN = "isolated-service-test-token";
    private final AtomicInteger responseCode = new AtomicInteger(200);
    private final AtomicReference<String> responseBody = new AtomicReference<>("[]");
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private final List<String> paths = new CopyOnWriteArrayList<>();
    private final List<String> tokens = new CopyOnWriteArrayList<>();

    @BeforeEach void startIsolatedHttpRecipient() throws Exception {
        recipient = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        recipient.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().toString());
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            tokens.add(String.valueOf(exchange.getRequestHeaders().getFirst("X-OYL-SERVICE-TOKEN")));
            byte[] bytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(responseCode.get(), bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        recipient.start();
        url = "http://127.0.0.1:" + recipient.getAddress().getPort();
    }
    @AfterEach void stopOnlyTestRecipient() { recipient.stop(0); }

    @Test void unavailableNotificationsDoNotPreventPurchaseAndOutboxRetriesSameEventUntilAcknowledged() throws Exception {
        responseCode.set(503);
        int stock = jdbc.queryForObject("SELECT stock FROM products WHERE id=1", Integer.class);
        var order = orders.create(new OrderService.CreateOrderRequest(UUID.randomUUID().toString(), "cliente-outbox",
            List.of(new OrderService.LineRequest(1, 1)), "PICKUP", "", ""));
        assertEquals(stock - 1, jdbc.queryForObject("SELECT stock FROM products WHERE id=1", Integer.class));
        assertEquals(order.id(), orders.byId(order.id()).id());
        var publisher = new OutboxPublisher(jdbc, url, TOKEN);
        publisher.flush();
        assertEquals(1, bodies.size());
        assertNull(jdbc.queryForObject("SELECT delivered_at FROM integration_outbox WHERE order_id=?", Long.class, order.id()));
        responseCode.set(200);
        publisher.flush();
        assertEquals(2, bodies.size()); assertEquals(bodies.get(0), bodies.get(1));
        var event = new ObjectMapper().readTree(bodies.get(1));
        assertEquals(order.id(), event.get("orderId").asText());
        assertEquals("PENDING", event.get("status").asText());
        assertEquals("ORDER_CREATED", event.get("kind").asText());
        assertNotNull(jdbc.queryForObject("SELECT delivered_at FROM integration_outbox WHERE order_id=?", Long.class, order.id()));
        publisher.flush(); assertEquals(2, bodies.size());
        assertEquals(List.of("/api/notifications/events", "/api/notifications/events"), paths);
        assertEquals(List.of(TOKEN, TOKEN), tokens);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM integration_outbox WHERE order_id=?", Integer.class, order.id()));
        assertEquals(stock - 1, jdbc.queryForObject("SELECT stock FROM products WHERE id=1", Integer.class));
    }

    @Test void pickupQueriesAreActuallyDelegatedOverHttpAndPreserveUtf8AndCoordinates() {
        responseBody.set("""
            [{"id":"nunoa","title":"Punto Ñuñoa (demo)","reference":"Plaza Ñuñoa",
              "schedule":"Horario de ejemplo","latitude":-33.4554,"longitude":-70.5982}]
            """);
        var proxy = new PickupProxyController(url, TOKEN, mock(AdminController.class));
        var point = proxy.all().get(0);
        assertEquals("Punto Ñuñoa (demo)", point.title()); assertEquals("Plaza Ñuñoa", point.reference());
        assertEquals(-33.4554, point.latitude()); assertEquals(-70.5982, point.longitude());
        assertEquals(List.of("/api/pickup-points"), paths);
        responseCode.set(404);
        assertEquals(404, assertThrows(ResponseStatusException.class, proxy::all).getStatusCode().value());
        recipient.stop(0);
        assertEquals(503, assertThrows(ResponseStatusException.class, proxy::all).getStatusCode().value());
    }

    @Test void orderEventsDelegateByRemoteUuidWithInternalHeaderAndFailClearlyWhenUnavailable() {
        var validation = mock(OrderService.class);
        String id = UUID.randomUUID().toString();
        responseBody.set("[{\"eventId\":\"created\",\"orderId\":\"" + id +
            "\",\"kind\":\"ORDER_CREATED\",\"status\":\"PENDING\",\"createdAt\":1234,\"read\":false}]");
        var gateway = new OrderEventsController(validation, url, TOKEN);
        var events = gateway.list(id);
        assertEquals(id, events.get(0).orderId()); assertEquals("ORDER_CREATED", events.get(0).kind());
        assertEquals("PENDING", events.get(0).status()); assertFalse(events.get(0).read());
        assertEquals(List.of("/api/notifications/events?orderId=" + id), paths);
        assertEquals(List.of(TOKEN), tokens); verify(validation).byId(id);
        responseCode.set(503);
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> gateway.list(id)).getStatusCode().value());
        var noToken = new OrderEventsController(validation, url, "");
        int requests = paths.size();
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> noToken.list(id)).getStatusCode().value());
        assertEquals(requests, paths.size());
    }
}
