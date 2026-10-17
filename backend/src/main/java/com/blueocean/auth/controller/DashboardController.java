package com.blueocean.auth.controller;

import com.blueocean.auth.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard controller - accessible to any authenticated user.
 * Returns a personalized welcome message using the JWT-authenticated username.
 */
@RestController
@RequestMapping("/api/dashboard")
@Slf4j
@Tag(name = "Dashboard", description = "User dashboard - requires authentication")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    /**
     * GET /api/dashboard
     * Returns "Welcome <username>" for authenticated users.
     * Redirected here after successful login.
     *
     * @param authentication injected by Spring Security from JWT context
     * @return welcome message with username
     */
    @GetMapping
    @Operation(
        summary = "Dashboard",
        description = "Returns a personalized welcome message. Requires a valid JWT token."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard accessed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<String>> dashboard(Authentication authentication) {
        String username = authentication.getName();
        log.info("Dashboard accessed by: {}", username);

        String welcomeMessage = "Welcome " + username;
        return ResponseEntity.ok(ApiResponse.success(welcomeMessage, welcomeMessage));
    }
}
