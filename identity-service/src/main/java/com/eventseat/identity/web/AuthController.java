package com.eventseat.identity.web;

import com.eventseat.identity.service.AuthService;
import com.eventseat.identity.web.dto.AuthResponse;
import com.eventseat.identity.web.dto.LoginRequest;
import com.eventseat.identity.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse resp = authService.register(request);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse resp = authService.login(request);
        return ResponseEntity.ok(resp);
    }
}
