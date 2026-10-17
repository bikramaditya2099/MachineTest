package com.blueocean.auth.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Service that maintains a blacklist of invalidated JWT tokens.
 * When a user logs out, their token is added to this set.
 * The JwtAuthFilter checks this blacklist before processing requests.
 *
 * Note: This is an in-memory set. In production, use Redis for
 * distributed blacklisting with TTL matching token expiration.
 */
@Service
public class JwtBlacklistService {

    /** Thread-safe set of invalidated token strings */
    private final Set<String> blacklistedTokens =
            Collections.synchronizedSet(new HashSet<>());

    /**
     * Adds a JWT token to the blacklist (on logout).
     *
     * @param token the JWT token string to invalidate
     */
    public void blacklist(String token) {
        blacklistedTokens.add(token);
    }

    /**
     * Checks if a JWT token has been blacklisted (logged out).
     *
     * @param token the JWT token string to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}
