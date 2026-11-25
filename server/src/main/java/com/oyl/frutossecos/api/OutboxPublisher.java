package com.oyl.frutossecos.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

/**
 * Envía eventos a otro microservicio sin afectar transacciones de compras.
 * Al caer el destino los eventos permanecen pendientes para el siguiente intento.
 */
@Component
public class OutboxPublisher {
    private final JdbcTemplate jdbc;
    private final RestClient client;
    private final String token;

    public OutboxPublisher(JdbcTemplate jdbc,
            @Value("${oyl.notifications.url:http://127.0.0.1:8082}") String url,
            @Value("${OYL_SERVICE_TOKEN:}") String token) {
        this.jdbc = jdbc;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1500);
        requestFactory.setReadTimeout(2500);
        this.client = RestClient.builder().baseUrl(url).requestFactory(requestFactory).build();
        this.token = token;
    }

    public record Pending(String eventId, String orderId, String kind,
                          String status, long createdAt) {}

    @Scheduled(fixedDelayString = "${oyl.notifications.flush-ms:2000}")
    public void flush() {
        if (token.length() < 16) return; // Sin token no enviar ni fingir sincronización.
        List<Pending> pending = jdbc.query("""
            SELECT event_id,order_id,kind,status,created_at
            FROM integration_outbox WHERE delivered_at IS NULL
            ORDER BY created_at,event_id LIMIT 30
            """, (rs, i) -> new Pending(rs.getString(1), rs.getString(2),
                    rs.getString(3), rs.getString(4), rs.getLong(5)));
        for (Pending event : pending) {
            try {
                client.post().uri("/api/notifications/events")
                    .header("X-OYL-SERVICE-TOKEN", token)
                    .body(event).retrieve().toBodilessEntity();
                jdbc.update("UPDATE integration_outbox SET delivered_at=? WHERE event_id=? AND delivered_at IS NULL",
                    System.currentTimeMillis(), event.eventId());
            } catch (RestClientException unavailable) {
                // Preservamos evento y repetimos luego. En el destino eventId es único.
                break;
            }
        }
    }
}
