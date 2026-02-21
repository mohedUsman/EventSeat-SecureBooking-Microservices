package com.eventseat.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eventseat.order.domain.OrderEntity;
import com.eventseat.order.domain.PaymentAttemptEntity;
import com.eventseat.order.repository.IdempotencyKeyJdbcRepository;
import com.eventseat.order.repository.InventoryJdbcRepository;
import com.eventseat.order.repository.OrdersJdbcRepository;
import com.eventseat.order.repository.PaymentAttemptJdbcRepository;
import com.eventseat.order.web.dto.OrderCreateRequest;
import com.eventseat.order.web.dto.OrderResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final OrdersJdbcRepository ordersRepo;
    private final PaymentAttemptJdbcRepository paymentRepo;
    private final IdempotencyKeyJdbcRepository idemRepo;
    private final InventoryJdbcRepository inventoryRepo;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrdersJdbcRepository ordersRepo,
            PaymentAttemptJdbcRepository paymentRepo,
            IdempotencyKeyJdbcRepository idemRepo,
            InventoryJdbcRepository inventoryRepo,
            ObjectMapper objectMapper) {
        this.ordersRepo = ordersRepo;
        this.paymentRepo = paymentRepo;
        this.idemRepo = idemRepo;
        this.inventoryRepo = inventoryRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse createOrder(String idemKey, OrderCreateRequest req, Jwt jwt) {
        validateOwnershipOrAdmin(jwt, req.getAttendeeId());
        if (idemKey == null || idemKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Idempotency-Key header");
        }
        // stable request signature (attendee,event,sorted
        // seats,currency,holdId,simulate)
        List<Long> sortedSeatIds = new ArrayList<>(req.getSeatIds());
        sortedSeatIds.sort(Comparator.naturalOrder());
        String canonical = String.format(Locale.ROOT, "aid=%d|eid=%d|seats=%s|cur=%s|hold=%d|sim=%s",
                req.getAttendeeId(), req.getEventId(), sortedSeatIds, req.getCurrency(),
                req.getHoldId(), req.getSimulate() == null ? "" : req.getSimulate().trim().toLowerCase(Locale.ROOT));
        String requestHash = Integer.toHexString(canonical.hashCode());

        // Try to insert idempotency record; if exists, return stored result when
        // available.
        boolean inserted = idemRepo.tryInsert(idemKey, requestHash);
        if (!inserted) {
            // Key exists - return stored response (if present) or 409 if mismatch
            var existing = idemRepo.findByKey(idemKey)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key in use"));
            if (!Objects.equals(existing.getRequestHash(), requestHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency key re-used with different payload");
            }
            String cached = existing.getResponseJson();
            if (cached == null || cached.isBlank()) {
                // Another in-flight request with same key
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Request with same Idempotency-Key is in flight");
            }
            try {
                return objectMapper.readValue(cached, OrderResponse.class);
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse cached response");
            }
        }

        // Validate hold
        InventoryJdbcRepository.HoldRow hold = inventoryRepo.getActiveHold(req.getHoldId());
        if (hold == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hold not found");
        }
        if (!"ACTIVE".equals(hold.status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hold is not ACTIVE");
        }
        if (!Objects.equals(hold.attendeeId, req.getAttendeeId()) || !Objects.equals(hold.eventId, req.getEventId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hold does not belong to attendee or event");
        }
        List<Long> holdSeatIds = splitCsv(hold.seatIdsCsv);
        if (!new ArrayList<>(holdSeatIds).containsAll(sortedSeatIds) || sortedSeatIds.size() != holdSeatIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requested seats do not match hold");
        }

        // Validate seats are HELD and compute total amount
        Map<Long, BigDecimal> prices = inventoryRepo.getHeldSeatPrices(req.getEventId(), sortedSeatIds);
        if (prices.size() != sortedSeatIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "One or more seats are not HELD");
        }
        BigDecimal amount = sortedSeatIds.stream()
                .map(prices::get)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b == null ? BigDecimal.ZERO : b));

        // Insert order PENDING
        String seatIdsCsv = joinCsv(sortedSeatIds);
        Long orderId = ordersRepo.insertPending(req.getAttendeeId(), req.getEventId(), amount, req.getCurrency(),
                seatIdsCsv);
        if (orderId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create order");
        }

        // Mock payment
        String simulate = req.getSimulate() == null ? "" : req.getSimulate().trim().toLowerCase(Locale.ROOT);
        if ("decline".equals(simulate)) {
            paymentRepo.insertAttempt(orderId, PaymentAttemptEntity.Type.AUTHORIZE,
                    PaymentAttemptEntity.Status.DECLINED, "mock-decline");
            // Leave seats HELD for user to retry or release hold manually
            OrderResponse resp = toResponse(orderId, req.getAttendeeId(), req.getEventId(), sortedSeatIds, amount,
                    req.getCurrency(),
                    OrderEntity.State.PENDING);
            cacheAndReturn(idemKey, requestHash, resp);
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Payment declined (mock)");
        } else if ("timeout".equals(simulate)) {
            paymentRepo.insertAttempt(orderId, PaymentAttemptEntity.Type.AUTHORIZE, PaymentAttemptEntity.Status.TIMEOUT,
                    "mock-timeout");
            OrderResponse resp = toResponse(orderId, req.getAttendeeId(), req.getEventId(), sortedSeatIds, amount,
                    req.getCurrency(),
                    OrderEntity.State.PENDING);
            cacheAndReturn(idemKey, requestHash, resp);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Payment timeout (mock)");
        } else {
            // Success path: AUTHORIZE + CAPTURE
            paymentRepo.insertAttempt(orderId, PaymentAttemptEntity.Type.AUTHORIZE, PaymentAttemptEntity.Status.SUCCESS,
                    null);
            paymentRepo.insertAttempt(orderId, PaymentAttemptEntity.Type.CAPTURE, PaymentAttemptEntity.Status.SUCCESS,
                    null);
        }

        // Transition seats HELD -> SOLD
        int sold = inventoryRepo.updateSeatsToSold(req.getEventId(), sortedSeatIds);
        if (sold != sortedSeatIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not mark all seats as SOLD");
        }

        // Update order state -> CONFIRMED
        ordersRepo.updateState(orderId, OrderEntity.State.CONFIRMED);

        OrderResponse resp = toResponse(orderId, req.getAttendeeId(), req.getEventId(), sortedSeatIds, amount,
                req.getCurrency(),
                OrderEntity.State.CONFIRMED);
        cacheAndReturn(idemKey, requestHash, resp);
        return resp;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id, Jwt jwt) {
        OrderEntity e = ordersRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        validateOwnershipOrAdmin(jwt, e.getAttendeeId());
        return toResponse(e.getId(), e.getAttendeeId(), e.getEventId(), splitCsv(e.getSeatIdsCsv()),
                e.getAmount(), e.getCurrency(), e.getState());
    }

    private void validateOwnershipOrAdmin(Jwt jwt, Long attendeeId) {
        Long uid = extractUid(jwt);
        boolean isAdmin = hasRole(jwt, "ADMIN");
        if (!isAdmin && !Objects.equals(uid, attendeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner or admin");
        }
    }

    private void cacheAndReturn(String idemKey, String requestHash, OrderResponse resp) {
        try {
            String json = objectMapper.writeValueAsString(resp);
            idemRepo.storeResponse(idemKey, resp.getId(), json);
        } catch (JsonProcessingException e) {
            // Non-fatal; idempotency replay won't work but request succeeded
        }
    }

    private OrderResponse toResponse(Long id, Long attendeeId, Long eventId, List<Long> seatIds,
            BigDecimal amount, String currency, OrderEntity.State state) {
        OrderResponse resp = new OrderResponse();
        resp.setId(id);
        resp.setAttendeeId(attendeeId);
        resp.setEventId(eventId);
        resp.setSeatIds(new ArrayList<>(seatIds));
        resp.setAmount(amount);
        resp.setCurrency(currency);
        resp.setState(state.name());
        resp.setCreatedAt(OffsetDateTime.now());
        resp.setUpdatedAt(OffsetDateTime.now());
        return resp;
    }

    private List<Long> splitCsv(String csv) {
        if (csv == null || csv.isBlank())
            return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private String joinCsv(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private Long extractUid(Jwt jwt) {
        if (jwt == null)
            return null;
        Object uid = jwt.getClaim("uid");
        if (uid instanceof Number n)
            return n.longValue();
        if (uid instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignore) {
            }
        }
        return null;
    }

    private boolean hasRole(Jwt jwt, String role) {
        if (jwt == null)
            return false;
        String csv = jwt.getClaimAsString("roles");
        if (csv == null || csv.isBlank())
            return false;
        String want = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        for (String p : csv.split(",")) {
            String r = p.trim();
            if (r.equalsIgnoreCase(want) || r.equalsIgnoreCase("ROLE_" + want)) {
                return true;
            }
        }
        return false;
    }
}
