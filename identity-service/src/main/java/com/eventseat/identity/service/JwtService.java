package com.eventseat.identity.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${security.jwt.issuer:eventseat-identity}")
    private String issuer;

    @Value("${security.jwt.ttl-minutes:30}")
    private long ttlMinutes;

    @Value("${security.jwt.secret:}")
    private String propertySecret;

    /**
     * Reads JWT secret from environment variable JWT_SECRET first, then system
     * property security.jwt.secret,
     * and finally Spring property security.jwt.secret if present. Requires a
     * minimum of 32 characters for HS256.
     */
    private Key resolveSigningKey() {
        String env = System.getenv("JWT_SECRET");
        String sys = System.getProperty("security.jwt.secret");
        String secret = env != null ? env : (sys != null ? sys : propertySecret);
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret is missing or too short. Set env JWT_SECRET to a 32+ char value.");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String email, String rolesCsv) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttlMinutes, ChronoUnit.MINUTES);
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("email", email);
        claims.put("roles", rolesCsv);

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(resolveSigningKey())
                .compact();
    }
}
