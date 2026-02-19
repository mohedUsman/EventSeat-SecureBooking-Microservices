package com.eventseat.identity.service;

import com.eventseat.identity.domain.UserEntity;
import com.eventseat.identity.repository.UserRepository;
import com.eventseat.identity.web.dto.AuthResponse;
import com.eventseat.identity.web.dto.LoginRequest;
import com.eventseat.identity.web.dto.RegisterRequest;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        String roles = (req.getRolesCsv() == null || req.getRolesCsv().isBlank())
                ? "ATTENDEE"
                : sanitizeRoles(req.getRolesCsv());

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRolesCsv(roles);
        user.setStatus(UserEntity.Status.ACTIVE);
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRolesCsv());
        Instant expiresAt = Instant.now().plusSeconds(60L * getTtlMinutesDefault(30));
        return new AuthResponse(token, expiresAt, user.getId(), user.getEmail(), user.getRolesCsv());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = normalizeEmail(req.getEmail());
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getStatus() != UserEntity.Status.ACTIVE) {
            throw new IllegalStateException("User not active");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRolesCsv());
        Instant expiresAt = Instant.now().plusSeconds(60L * getTtlMinutesDefault(30));
        return new AuthResponse(token, expiresAt, user.getId(), user.getEmail(), user.getRolesCsv());
    }

    private String normalizeEmail(String email) {
        if (email == null)
            return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeRoles(String rolesCsv) {
        // Very light normalization; full validation will come with RBAC policies
        return rolesCsv.replace(" ", "");
    }

    private long getTtlMinutesDefault(long def) {
        // The JwtService controls actual TTL in token; this is only for response hint
        return def;
    }
}
