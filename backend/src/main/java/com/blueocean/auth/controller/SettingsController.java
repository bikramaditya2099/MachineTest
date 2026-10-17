package com.blueocean.auth.controller;

import com.blueocean.auth.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Settings controller - accessible ONLY to ADMINISTRATOR role.
 * Standard users receive 403 Forbidden when accessing this endpoint.
 */
@RestController
@RequestMapping("/api/settings")
@Slf4j
@Tag(name = "Settings", description = "Admin-only settings endpoint - requires ADMINISTRATOR role")
@SecurityRequirement(name = "bearerAuth")
@lombok.RequiredArgsConstructor
public class SettingsController {

    private final com.blueocean.auth.service.CountryValidationService countryValidationService;

    /**
     * GET /api/settings
     * Returns application settings. Only accessible to users with ADMINISTRATOR role.
     * Standard users receive HTTP 403 Forbidden.
     *
     * @param authentication injected by Spring Security from JWT context
     * @return settings data for admin users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(
        summary = "Admin Settings",
        description = "Returns system settings. ONLY accessible to users with ADMINISTRATOR role. " +
                      "Standard users receive 403 Forbidden."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Settings retrieved")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - not an Administrator")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings(Authentication authentication) {
        String adminUsername = authentication.getName();
        log.info("Settings accessed by admin: {}", adminUsername);

        // Example settings data for the admin
        Map<String, Object> settings = Map.of(
                "accessedBy", adminUsername,
                "role", "ADMINISTRATOR",
                "systemName", "JWT Auth System",
                "version", "1.0.0",
                "userStorageType", "In-Memory (ConcurrentHashMap)",
                "tokenExpiry", "24 hours",
                "passwordPolicy", Map.of(
                        "minLength", 8,
                        "maxLength", 12,
                        "requireUppercase", true,
                        "requireLowercase", true,
                        "requireDigit", true,
                        "requireSpecialChar", true
                ),
                "restrictedCountries", countryValidationService.getRestrictedCountryNames()
        );

        return ResponseEntity.ok(ApiResponse.success("Settings retrieved successfully", settings));
    }
}
