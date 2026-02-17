package com.eventseat.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                // Allow read-only APIs without auth for Milestone 1
                                                .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                                                // Allow Swagger/OpenAPI/Actuator/Errors for convenience
                                                .requestMatchers(
                                                                "/v3/api-docs",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/actuator/**",
                                                                "/error")
                                                .permitAll()
                                                // All non-GET API operations require organizer or admin
                                                .requestMatchers(HttpMethod.POST, "/api/v1/**")
                                                .hasAnyRole("ORGANIZER", "ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/**")
                                                .hasAnyRole("ORGANIZER", "ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/**")
                                                .hasAnyRole("ORGANIZER", "ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/**")
                                                .hasAnyRole("ORGANIZER", "ADMIN")
                                                .anyRequest().authenticated())
                                .httpBasic(Customizer.withDefaults());

                return http.build();
        }

        @Bean
        public UserDetailsService userDetailsService(PasswordEncoder encoder) {
                UserDetails admin = User.withUsername("admin")
                                .password(encoder.encode("Admin@123"))
                                .roles("ADMIN")
                                .build();
                UserDetails organizer = User.withUsername("organizer")
                                .password(encoder.encode("Organizer@123"))
                                .roles("ORGANIZER")
                                .build();
                UserDetails attendee = User.withUsername("attendee")
                                .password(encoder.encode("Attendee@123"))
                                .roles("ATTENDEE")
                                .build();
                return new InMemoryUserDetailsManager(admin, organizer, attendee);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
