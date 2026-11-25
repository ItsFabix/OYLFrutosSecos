package com.oyl.frutossecos.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Consulta de los eventos remotos por UUID de pedido; no expone la clave interna. */
@RestController
@RequestMapping("/api/orders")
public class OrderEventsController {
    private final OrderService orders;
    private final RestClient client;
    private final String token;
    public record Event(String eventId, String orderId, String kind, String status,
                        long createdAt, boolean read) {}

    public OrderEventsController(OrderService orders,
            @Value("${oyl.notifications.url:http://127.0.0.1:8082}") String url,
            @Value("${OYL_SERVICE_TOKEN:}") String token) {
        this.orders = orders;
        this.client = RestClient.builder().baseUrl(url).build();
        this.token = token;
    }

    @GetMapping("/{id}/notifications")
    public List<Event> list(@PathVariable String id) {
        orders.byId(id); // 404 si el pedido no existe; UUID no predecible.
        if (token.length() < 16) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Microservicio de notificaciones no configurado");
        try {
            return client.get().uri(uri -> uri.path("/api/notifications/events")
                    .queryParam("orderId", id).build())
                    .header("X-OYL-SERVICE-TOKEN", token).retrieve()
                    .body(new ParameterizedTypeReference<List<Event>>() {});
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Microservicio de notificaciones no disponible");
        }
    }
}
