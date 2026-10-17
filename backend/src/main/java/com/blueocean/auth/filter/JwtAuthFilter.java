package com.blueocean.auth.filter;

import com.blueocean.auth.service.JwtBlacklistService;
import com.blueocean.auth.service.impl.UserServiceImpl;
import com.blueocean.auth.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that intercepts every request once.
 * Extracts the Bearer token from the Authorization header,
 * validates it, and sets the security context if valid.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserServiceImpl userService;
    private final JwtBlacklistService blacklistService;

    /**
     * Core filter logic:
     * 1. Check for "Authorization: Bearer <token>" header
     * 2. Check if token is blacklisted (logged out)
     * 3. Extract username and validate token
     * 4. Set authentication in SecurityContext
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // If no Bearer token, skip this filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token (remove "Bearer " prefix)
        jwt = authHeader.substring(7);

        // Reject blacklisted (logged out) tokens
        if (blacklistService.isBlacklisted(jwt)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token has been invalidated. Please login again.\"}");
            return;
        }

        try {
            username = jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            // Invalid token format
            filterChain.doFilter(request, response);
            return;
        }

        // If username found and no authentication set yet
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                // Create authentication token and set in context
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
