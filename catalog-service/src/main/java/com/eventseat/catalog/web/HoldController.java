package com.eventseat.catalog.web;

import com.eventseat.catalog.service.HoldService;
import com.eventseat.catalog.web.dto.HoldCreateRequest;
import com.eventseat.catalog.web.dto.HoldResponse;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/holds")
public class HoldController {

    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    // ATTENDEE creates a hold (requires Bearer token with ATTENDEE role)
    @PostMapping
    public HoldResponse create(@Valid @RequestBody HoldCreateRequest req, @AuthenticationPrincipal Jwt jwt) {
        return holdService.createHold(req, jwt);
    }

    // ATTENDEE (owner) or ADMIN can view a hold
    @GetMapping("/{id}")
    public HoldResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return holdService.getHold(id, jwt);
    }

    // ATTENDEE (owner) or ADMIN can release a hold
    @DeleteMapping("/{id}")
    public void release(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        holdService.releaseHold(id, jwt);
    }
}
