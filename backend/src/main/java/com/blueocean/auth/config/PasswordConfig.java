package com.blueocean.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password encoding configuration, kept separate from SecurityConfig
 * to avoid a circular dependency (SecurityConfig -> JwtAuthFilter -> UserServiceImpl -> PasswordEncoder).
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt password encoder with default strength (10 rounds).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
