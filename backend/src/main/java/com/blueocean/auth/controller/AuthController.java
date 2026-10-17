package com.blueocean.auth.controller;

import com.blueocean.auth.dto.*;
import com.blueocean.auth.model.User;
import com.blueocean.auth.service.CountryValidationService;
import com.blueocean.auth.service.JwtBlacklistService;
import com.blueocean.auth.service.impl.UserServiceImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.blueocean.auth.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations:
 * register, verify OTP, login, logout.
 *
 * All endpoints under /api/auth/** are publicly accessible (no JWT required).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User registration, OTP verification, login, and logout")
public class AuthController {

    private final UserServiceImpl userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtBlacklistService blacklistService;
    private final CountryValidationService countryValidationService;

    /**
     * POST /api/auth/register
     * Registers a new user. Validates country, email, password, and uniqueness.
     * Returns a 6-digit OTP for verification.
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account. Country is validated via IP. OTP is returned for verification."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Country not allowed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User already exists")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        AuthResponse response = userService.register(request, ipAddress);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully. Please verify OTP.", response));
    }

    /**
     * GET /api/auth/countries
     * Returns all countries with a flag indicating whether sign-up is restricted.
     */
    @GetMapping("/countries")
    @Operation(
        summary = "List countries",
        description = "Returns all countries (ISO code, name, restricted flag) for the sign-up dropdown."
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCountries() {
        return ResponseEntity.ok(ApiResponse.success("Countries retrieved", countryValidationService.getAllCountries()));
    }

    /**
     * GET /api/auth/detect-country
     * Detects the caller's country from their IP address (used to pre-select the dropdown).
     * Returns a null code when it cannot be determined (e.g. localhost).
     */
    @GetMapping("/detect-country")
    @Operation(
        summary = "Detect country from IP",
        description = "Resolves the caller's country from their IP address. Code is null if undetectable."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> detectCountry(HttpServletRequest httpRequest) {
        String code = countryValidationService.getCountryCode(getClientIpAddress(httpRequest));
        Map<String, String> result = new HashMap<>();
        boolean known = code != null && countryValidationService.isValidCountryCode(code);
        result.put("code", known ? code.toUpperCase() : null);
        result.put("name", known ? countryValidationService.getCountryName(code) : null);
        return ResponseEntity.ok(ApiResponse.success("Country detected", result));
    }

    /**
     * POST /api/auth/verify-otp
     * Verifies the OTP sent during registration. Activates the user account.
     */
    @PostMapping("/verify-otp")
    @Operation(
        summary = "Verify OTP",
        description = "Verifies the OTP generated during registration. Enables the user account."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP verified")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid OTP")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        userService.verifyOtp(request.getUsername(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully. You can now login."));
    }

    /**
     * POST /api/auth/login
     * Authenticates the user and returns a JWT token.
     * Returns "Welcome <username>" on success, "Login Failed" on failure.
     */
    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticates credentials and returns a JWT token. Use the token for all secured endpoints."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Login Failed")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Authenticate via Spring Security (throws BadCredentialsException on failure)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = (User) authentication.getPrincipal();

            // Generate JWT with username and role
            String token = jwtUtil.generateToken(user.getUsername(), user.getUserType().name());

            AuthResponse authResponse = AuthResponse.builder()
                    .token(token)
                    .message("Welcome " + user.getUsername())
                    .userType(user.getUserType())
                    .build();

            log.info("User logged in successfully: {}", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Welcome " + user.getUsername(), authResponse));

        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for username: {}", request.getUsername());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Login Failed"));
        }
    }

    /**
     * POST /api/auth/logout
     * Invalidates the user's JWT token by adding it to the blacklist.
     * Returns "<username> Logged Out" on success.
     */
    @PostMapping("/logout")
    @Operation(
        summary = "User logout",
        description = "Invalidates the JWT token. The token cannot be used for any further requests."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid token")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No valid token provided"));
        }

        String token = authHeader.substring(7);
        String username = userService.logout(token);

        return ResponseEntity.ok(ApiResponse.success(username + " Logged Out"));
    }

    /**
     * Extracts the real client IP from request headers.
     * Handles proxies/load balancers via X-Forwarded-For header.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
