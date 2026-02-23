package com.eventseat.identity.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${security.jwt.secret:local-dev-secret-0123456789abcdef-0123456789}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/actuator/**",
                                "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // PII profile endpoints must be authenticated
                        .requestMatchers("/api/v1/profile/**").authenticated()
                        // Default: require auth for other backend endpoints
                        .anyRequest().authenticated())
                // Enable JWT resource server so @AuthenticationPrincipal Jwt is populated
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Align with JwtService secret resolution to avoid signature mismatches:
        // Prefer env JWT_SECRET, then system property security.jwt.secret, then Spring
        // property.
        String env = System.getenv("JWT_SECRET");
        String sys = System.getProperty("security.jwt.secret");
        String secret = env != null ? env : (sys != null ? sys : jwtSecret);
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret missing or too short for HS256. Set JWT_SECRET or security.jwt.secret (32+ chars).");
        }
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
