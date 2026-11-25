package com.oyl.frutossecos.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Puerta de entrada estable para la app Android (8080).
 * Delegación HTTP real al microservicio de puntos de retiro (8081).
 */
@RestController
public class PickupProxyController {
    private final RestClient client;
    private final String serviceToken;
    private final AdminController admin;

    public PickupProxyController(
            @Value("${oyl.pickup.url:http://127.0.0.1:8081}") String url,
            @Value("${OYL_SERVICE_TOKEN:}") String serviceToken,
            AdminController admin) {
        this.client = RestClient.builder().baseUrl(url).build();
        this.serviceToken = serviceToken;
        this.admin = admin;
    }

    public record Point(String id, String title, String reference, String schedule,
                        double latitude, double longitude) {}
    public record PointInput(String title, String reference, String schedule,
                             Double latitude, Double longitude) {}

    private static ResponseStatusException unavailable(RestClientException e) {
        if (e instanceof RestClientResponseException response)
            return new ResponseStatusException(HttpStatus.valueOf(response.getStatusCode().value()),
                "Microservicio de puntos de retiro rechazó la solicitud");
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Microservicio de puntos de retiro no disponible");
    }

    @GetMapping("/api/pickup-points")
    public List<Point> all() {
        try {
            return client.get().uri("/api/pickup-points").retrieve()
                .body(new ParameterizedTypeReference<List<Point>>() {});
        } catch (RestClientException e) { throw unavailable(e); }
    }

    @PostMapping("/api/admin/pickup-points/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public Point create(@PathVariable String id, @RequestBody PointInput input,
            @RequestHeader(value = "X-OYL-ADMIN-KEY", required = false) String key) {
        admin.authorize(key);
        try {
            return client.post().uri("/api/pickup-points/{id}", id)
                .header("X-OYL-SERVICE-TOKEN", serviceToken).body(input)
                .retrieve().body(Point.class);
        } catch (RestClientException e) { throw unavailable(e); }
    }

    @PutMapping("/api/admin/pickup-points/{id}")
    public Point update(@PathVariable String id, @RequestBody PointInput input,
            @RequestHeader(value = "X-OYL-ADMIN-KEY", required = false) String key) {
        admin.authorize(key);
        try {
            return client.put().uri("/api/pickup-points/{id}", id)
                .header("X-OYL-SERVICE-TOKEN", serviceToken).body(input)
                .retrieve().body(Point.class);
        } catch (RestClientException e) { throw unavailable(e); }
    }

    @DeleteMapping("/api/admin/pickup-points/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id,
            @RequestHeader(value = "X-OYL-ADMIN-KEY", required = false) String key) {
        admin.authorize(key);
        try {
            client.delete().uri("/api/pickup-points/{id}", id)
                .header("X-OYL-SERVICE-TOKEN", serviceToken)
                .retrieve().toBodilessEntity();
        } catch (RestClientException e) { throw unavailable(e); }
    }
}
