package com.eventseat.identity.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loads the AES-GCM key for PII encryption.
 * Priority:
 * 1) Environment variable PII_ENC_KEY (Base64-encoded 32 bytes)
 * 2) Derive from security.jwt.secret via SHA-256 (deterministic dev fallback)
 */
@Configuration
public class PiiCryptoConfig {

    private static final Logger log = LoggerFactory.getLogger(PiiCryptoConfig.class);

    @Value("${security.jwt.secret:}")
    private String jwtSecret;

    private SecretKey deriveFromJwtSecret(String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(secret.getBytes(StandardCharsets.UTF_8));
            // use full 32 bytes as AES-256 key
            return new SecretKeySpec(hash, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private SecretKey loadKey() {
        String env = System.getenv("PII_ENC_KEY");
        if (env != null && !env.isBlank()) {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(env.trim());
                if (keyBytes.length != 32) {
                    throw new IllegalArgumentException("PII_ENC_KEY must be Base64-encoded 32 bytes (256-bit)");
                }
                log.info("Loaded PII encryption key from PII_ENC_KEY (environment).");
                return new SecretKeySpec(keyBytes, "AES");
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Failed to decode PII_ENC_KEY: " + ex.getMessage(), ex);
            }
        }
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            log.warn("PII_ENC_KEY not set; deriving PII key from security.jwt.secret (dev fallback).");
            return deriveFromJwtSecret(jwtSecret);
        }
        // Absolute last resort (should not happen given our configs)
        log.warn("PII_ENC_KEY and security.jwt.secret not set; generating ephemeral PII key (NOT RECOMMENDED).");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = md.digest(("ephemeral-" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot initialize PII key", e);
        }
    }

    @Bean
    ApplicationRunner initPiiKey() {
        return args -> {
            SecretKey key = loadKey();
            PiiKeyHolder.setKey(key);
            log.info("PII encryption key initialized (AES-256, {} bytes).",
                    key.getEncoded() == null ? "n/a" : key.getEncoded().length);
        };
    }
}
