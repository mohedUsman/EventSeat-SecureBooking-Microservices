package com.eventseat.catalog.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
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
                                                // Read-only APIs remain open in M1
                                                .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                                                // Swagger/OpenAPI/Actuator/error open
                                                .requestMatchers(
                                                                "/v3/api-docs",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/actuator/**",
                                                                "/error")
                                                .permitAll()
                                                // Write operations require ORGANIZER or ADMIN
                                                .requestMatchers(HttpMethod.POST, "/api/v1/**")
                                                .hasAnyRole("ORGANIZER", "ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/**")
                                                .hasAnyRole("ORGANIZER", "ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/**")
                                                .hasAnyRole("ORGANIZER", "ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/**")
                                                .hasAnyRole("ORGANIZER", "ADMIN")
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter())));

                return http.build();
        }

        @Bean
        public JwtDecoder jwtDecoder() {
                // HS256 symmetric key decoder using the local property by default
                SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                return NimbusJwtDecoder.withSecretKey(key).build();
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
                conv.setJwtGrantedAuthoritiesConverter(jwt -> extractAuthoritiesFromRolesClaim(jwt));
                return conv;
        }

        private Collection<GrantedAuthority> extractAuthoritiesFromRolesClaim(Jwt jwt) {
                List<GrantedAuthority> auths = new ArrayList<>();

                Map<String, Object> claims = jwt.getClaims();
                Object rolesObj = claims.get("roles");
                if (rolesObj instanceof String rolesCsv) {
                        String[] parts = rolesCsv.split(",");
                        for (String p : parts) {
                                String role = p.trim();
                                if (!role.isEmpty()) {
                                        String withPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                                        auths.add(new SimpleGrantedAuthority(withPrefix));
                                }
                        }
                }
                // Optionally, you could also include scope authorities if you add scopes later
                return auths;
        }
}
