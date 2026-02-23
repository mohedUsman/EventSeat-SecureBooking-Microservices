package com.eventseat.review.web;

import com.eventseat.review.service.ReviewService;
import com.eventseat.review.web.dto.ReviewDtos.EventReviewsResponse;
import com.eventseat.review.web.dto.ReviewDtos.ReviewCreateRequest;
import com.eventseat.review.web.dto.ReviewDtos.ReviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
@Validated
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    // Public read: event average + latest 20 APPROVED reviews
    @GetMapping("/events/{eventId}/reviews")
    public EventReviewsResponse getEventReviews(@PathVariable Long eventId) {
        return service.getEventReviews(eventId);
    }

    // Authenticated ATTENDEE only: create a review after attendance (PENDING for moderation)
    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
            @Valid @RequestBody ReviewCreateRequest req,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing_token");
        }
        // Enforce ATTENDEE role
        String roles = jwt.getClaimAsString("roles");
        boolean isAttendee = false;
        if (roles != null) {
            for (String p : roles.split(",")) {
                if ("ATTENDEE".equalsIgnoreCase(p.trim()) || "ROLE_ATTENDEE".equalsIgnoreCase(p.trim())) {
                    isAttendee = true;
                    break;
                }
            }
        }
        if (!isAttendee) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "attendee_role_required");
        }

        Long attendeeId = extractUid(jwt);
        String bearer = extractBearer(authorization);
        return service.createReview(attendeeId, bearer, req);
    }

    // ADMIN-only: approve a review
    @PatchMapping("/reviews/{id}/approve")
    public ReviewResponse approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        enforceAdmin(jwt);
        return service.approveReview(id);
    }

    // ADMIN-only: reject a review
    @PatchMapping("/reviews/{id}/reject")
    public ReviewResponse reject(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        enforceAdmin(jwt);
        return service.rejectReview(id);
    }

    private void enforceAdmin(Jwt jwt) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing_token");
        String csv = jwt.getClaimAsString("roles");
        if (csv == null || csv.isBlank()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "missing_roles");
        for (String p : csv.split(",")) {
            if ("ADMIN".equalsIgnoreCase(p.trim()) || "ROLE_ADMIN".equalsIgnoreCase(p.trim())) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin_role_required");
    }

    private Long extractUid(Jwt jwt) {
        Object uid = jwt.getClaim("uid");
        if (uid instanceof Number n) return n.longValue();
        if (uid instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignore) {}
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_uid_claim");
    }

    private String extractBearer(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing_bearer_token");
        }
        return authHeader.substring("Bearer ".length()).trim();
    }
}
