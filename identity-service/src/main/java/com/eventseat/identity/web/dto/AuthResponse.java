package com.eventseat.identity.web.dto;

import java.time.Instant;

public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Instant expiresAt;
    private Long userId;
    private String email;
    private String roles; // comma-separated

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, Instant expiresAt, Long userId, String email, String roles) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
        this.userId = userId;
        this.email = email;
        this.roles = roles;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }
}
