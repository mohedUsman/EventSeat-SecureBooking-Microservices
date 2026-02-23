package com.eventseat.identity.web;

import com.eventseat.identity.domain.AttendeeProfileEntity;
import com.eventseat.identity.repository.AttendeeProfileRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Minimal REST endpoints for encrypted AttendeeProfile.
 * - Owner (ATTENDEE) can read/update their own profile
 * - ADMIN can read any profile by userId
 *
 * Notes:
 * - PII is encrypted at rest via AesGcmStringConverter on the entity
 * - Responses return decrypted values only to the owner (or ADMIN)
 * - Avoid logging request/response bodies to keep PII out of logs
 */
@RestController
@RequestMapping("/api/v1/profile")
@Validated
public class AttendeeProfileController {

    private final AttendeeProfileRepository repo;

    public AttendeeProfileController(AttendeeProfileRepository repo) {
        this.repo = repo;
    }

    // Owner fetches their own profile (decrypted)
    @GetMapping("/me")
    public ProfileResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        Long uid = extractUid(jwt);
        AttendeeProfileEntity e = repo.findByUserId(uid).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        return toResponse(e);
    }

    // ADMIN fetches a specific user's profile
    @GetMapping("/{userId}")
    public ProfileResponse getProfileByUser(@PathVariable Long userId, @AuthenticationPrincipal Jwt jwt) {
        enforceAdmin(jwt);
        AttendeeProfileEntity e = repo.findByUserId(userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        return toResponse(e);
    }

    // Owner upserts their profile (create if absent, otherwise update)
    @PutMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public ProfileResponse upsertMyProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileRequest req) {
        Long uid = extractUid(jwt);
        // Upsert pattern
        Optional<AttendeeProfileEntity> existing = repo.findByUserId(uid);
        AttendeeProfileEntity e = existing.orElseGet(AttendeeProfileEntity::new);
        e.setUserId(uid);
        e.setName(nullIfBlank(req.getName()));
        e.setPhone(nullIfBlank(req.getPhone()));
        e.setAddress(nullIfBlank(req.getAddress()));
        AttendeeProfileEntity saved = repo.save(e);
        return toResponse(saved);
    }

    private ProfileResponse toResponse(AttendeeProfileEntity e) {
        ProfileResponse r = new ProfileResponse();
        r.setUserId(e.getUserId());
        // Return decrypted values (converter handles it); do not log them
        r.setName(e.getName());
        r.setPhone(e.getPhone());
        r.setAddress(e.getAddress());
        return r;
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private Long extractUid(Jwt jwt) {
        if (jwt == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        Object uid = jwt.getClaim("uid");
        if (uid instanceof Number n)
            return n.longValue();
        if (uid instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignore) {
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid uid claim");
    }

    private void enforceAdmin(Jwt jwt) {
        if (jwt == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        String csv = jwt.getClaimAsString("roles");
        if (csv == null || csv.isBlank())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing roles");
        for (String p : csv.split(",")) {
            if ("ADMIN".equalsIgnoreCase(p.trim()) || "ROLE_ADMIN".equalsIgnoreCase(p.trim()))
                return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
    }

    public static class ProfileResponse {
        private Long userId;
        private String name;
        private String phone;
        private String address;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    public static class ProfileRequest {
        @NotBlank(message = "name is required")
        @Size(max = 512)
        private String name;

        @NotBlank(message = "phone is required")
        @Size(max = 128)
        private String phone;

        @NotBlank(message = "address is required")
        @Size(max = 2000)
        private String address;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }
}
