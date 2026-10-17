package com.blueocean.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for JWT token generation and validation.
 * Uses HMAC-SHA256 (HS256) signing algorithm.
 * Token contains: subject (username), role, issued-at, expiration.
 */
@Component
public class JwtUtil {

    /** Secret key for signing tokens (loaded from application.yml) */
    @Value("${jwt.secret}")
    private String secretKey;

    /** Token validity period in milliseconds (default: 24 hours) */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Generates a JWT token for the given user with role claim.
     *
     * @param username  the authenticated user's username
     * @param role      the user's role (e.g., STANDARD_USER, ADMINISTRATOR)
     * @return signed JWT token string
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return buildToken(claims, username);
    }

    /**
     * Builds the JWT token with the given claims and subject.
     */
    private String buildToken(Map<String, Object> extraClaims, String subject) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token JWT token string
     * @return username stored in the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the role claim from a JWT token.
     *
     * @param token JWT token string
     * @return role string (e.g., "ADMINISTRATOR")
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * @param token JWT token string
     * @return expiration Date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract any claim from the token.
     *
     * @param token          JWT token string
     * @param claimsResolver function to extract a specific claim
     * @param <T>            type of the claim
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Validates whether the token belongs to the given user and is not expired.
     *
     * @param token       JWT token string
     * @param userDetails Spring Security UserDetails object
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Checks whether the JWT token is expired.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Parses and returns all claims from a JWT token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Decodes the base64-encoded secret and returns an HMAC key.
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
