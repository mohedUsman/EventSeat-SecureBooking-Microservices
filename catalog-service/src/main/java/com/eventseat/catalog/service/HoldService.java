package com.eventseat.catalog.service;

import com.eventseat.catalog.domain.HoldEntity;
import com.eventseat.catalog.repository.HoldJdbcRepository;
import com.eventseat.catalog.repository.SeatJdbcRepository;
import com.eventseat.catalog.web.HoldConflictException;
import com.eventseat.catalog.web.dto.HoldCreateRequest;
import com.eventseat.catalog.web.dto.HoldResponse;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HoldService {

    private static final int DEFAULT_TTL_MIN = 15;

    private final HoldJdbcRepository holdRepo;
    private final SeatJdbcRepository seatRepo;

    public HoldService(HoldJdbcRepository holdRepo, SeatJdbcRepository seatRepo) {
        this.holdRepo = holdRepo;
        this.seatRepo = seatRepo;
    }

    @Transactional
    public HoldResponse createHold(HoldCreateRequest req, Jwt jwt) {
        Long jwtUid = extractUid(jwt);
        boolean isAdmin = hasRole(jwt, "ADMIN");
        // Owner or ADMIN may create
        if (!isAdmin && !Objects.equals(jwtUid, req.getAttendeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "attendeeId does not match token uid");
        }

        if (req.getSeatIds() == null || req.getSeatIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seatIds is required");
        }

        int ttl = (req.getTtlMinutes() == null || req.getTtlMinutes() < 1) ? DEFAULT_TTL_MIN : req.getTtlMinutes();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(ttl, ChronoUnit.MINUTES);

        // Attempt to mark requested seats as HELD atomically (within tx)
        int updated = holdRepo.updateSeatsToHeld(req.getEventId(), req.getSeatIds());
        if (updated != req.getSeatIds().size()) {
            // Not all seats were AVAILABLE - build diagnostics and return informative 409
            List<Long> ids = req.getSeatIds();
            Map<Long, String> statuses = seatRepo.findStatusesForEventAndIds(req.getEventId(), ids);
            Map<Long, Object> diag = new HashMap<>();
            for (Long sid : ids) {
                if (!statuses.containsKey(sid)) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("reason", "not_found_or_wrong_event");
                    diag.put(sid, m);
                } else {
                    String s = statuses.get(sid);
                    if (!"AVAILABLE".equalsIgnoreCase(s)) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("reason", "not_available");
                        m.put("status", s);
                        diag.put(sid, m);
                    } else {
                        // Very rare: still AVAILABLE but update failed (race between read and update)
                        Map<String, Object> m = new HashMap<>();
                        m.put("reason", "race_condition_or_retried");
                        m.put("status", s);
                        diag.put(sid, m);
                    }
                }
            }
            throw new HoldConflictException("One or more seats are not AVAILABLE", diag);
        }

        String seatIdsCsv = toCsv(req.getSeatIds());
        long holdId = holdRepo.insertActive(req.getAttendeeId(), req.getEventId(), seatIdsCsv, now, expiresAt);
        if (holdId <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create hold");
        }

        HoldResponse resp = new HoldResponse();
        resp.setId(holdId);
        resp.setAttendeeId(req.getAttendeeId());
        resp.setEventId(req.getEventId());
        resp.setSeatIds(new ArrayList<>(req.getSeatIds()));
        resp.setStatus(HoldEntity.Status.ACTIVE.name());
        resp.setCreatedAt(now);
        resp.setExpiresAt(expiresAt);
        return resp;
    }

    @Transactional(readOnly = true)
    public HoldResponse getHold(Long id, Jwt jwt) {
        HoldEntity e = holdRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hold not found"));

        Long jwtUid = extractUid(jwt);
        boolean isAdmin = hasRole(jwt, "ADMIN");
        if (!isAdmin && !Objects.equals(jwtUid, e.getAttendeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this hold");
        }

        HoldResponse resp = new HoldResponse();
        resp.setId(e.getId());
        resp.setAttendeeId(e.getAttendeeId());
        resp.setEventId(e.getEventId());
        resp.setSeatIds(fromCsv(e.getSeatIdsCsv()));
        resp.setStatus(e.getStatus().name());
        resp.setCreatedAt(e.getCreatedAt());
        resp.setExpiresAt(e.getExpiresAt());
        return resp;
    }

    @Transactional
    public void releaseHold(Long id, Jwt jwt) {
        HoldEntity e = holdRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hold not found"));

        Long jwtUid = extractUid(jwt);
        boolean isAdmin = hasRole(jwt, "ADMIN");
        if (!isAdmin && !Objects.equals(jwtUid, e.getAttendeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to release this hold");
        }

        List<Long> seatIds = fromCsv(e.getSeatIdsCsv());
        // Release seats first
        holdRepo.updateSeatsToAvailable(e.getEventId(), seatIds);
        // Mark hold released (only if it was ACTIVE)
        holdRepo.markReleased(e.getId());
    }

    // Scheduled job will call this per expired hold (wrapped in its own tx)
    @Transactional
    public void expireAndRelease(HoldEntity e) {
        if (e.getStatus() != HoldEntity.Status.ACTIVE)
            return;
        List<Long> seatIds = fromCsv(e.getSeatIdsCsv());
        holdRepo.updateSeatsToAvailable(e.getEventId(), seatIds);
        holdRepo.markExpired(e.getId());
    }

    private String toCsv(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Long> fromCsv(String csv) {
        if (csv == null || csv.isBlank())
            return List.of();
        String[] parts = csv.split(",");
        List<Long> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            try {
                out.add(Long.parseLong(p.trim()));
            } catch (NumberFormatException ignore) {
            }
        }
        return out;
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
        String want = role == null ? "" : role.trim().toUpperCase();
        for (String p : csv.split(",")) {
            String r = p.trim();
            if (r.equalsIgnoreCase(want) || r.equalsIgnoreCase("ROLE_" + want)) {
                return true;
            }
        }
        return false;
    }
}
