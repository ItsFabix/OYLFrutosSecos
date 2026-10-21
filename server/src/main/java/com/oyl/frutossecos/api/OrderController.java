package com.oyl.frutossecos.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderService.OrderResponse create(@RequestBody OrderService.CreateOrderRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public OrderService.OrderResponse byId(@PathVariable String id) {
        return service.byId(id);
    }

    @ExceptionHandler(OrderService.StockRejectedException.class)
    public ResponseEntity<Map<String, String>> rejected(OrderService.StockRejectedException error) {
        // The transactional service has already rolled back. Bare/legacy 409s carry no such guarantee.
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header("X-OYL-Rejected-Request", error.requestId())
                .body(Map.of("requestId", error.requestId(), "message", error.getMessage()));
    }
}
