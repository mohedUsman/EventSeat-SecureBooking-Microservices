package com.eventseat.order.web;

import com.eventseat.order.service.OrderService;
import com.eventseat.order.web.dto.OrderCreateRequest;
import com.eventseat.order.web.dto.OrderResponse;
import com.eventseat.order.web.dto.OrderStateUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Create order idempotently. Requires "Idempotency-Key" header and ATTENDEE
    // role.
    @PostMapping
    public OrderResponse create(@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OrderCreateRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return orderService.createOrder(idempotencyKey, req, jwt);
    }

    // Get order by id (owner or ADMIN).
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.getOrder(id, jwt);
    }

    // Admin-only: transition order state (e.g., CHECKED_IN or COMPLETED) for local
    // testing
    @PatchMapping("/{id}/state")
    public OrderResponse updateState(
            @PathVariable Long id,
            @Valid @RequestBody OrderStateUpdateRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return orderService.updateState(id, req.getState(), jwt);
    }
}
