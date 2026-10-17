package com.blueocean.auth.service.impl;

import com.blueocean.auth.dto.AuthResponse;
import com.blueocean.auth.dto.RegisterRequest;
import com.blueocean.auth.exception.CountryNotAllowedException;
import com.blueocean.auth.exception.InvalidOtpException;
import com.blueocean.auth.exception.UserAlreadyExistsException;
import com.blueocean.auth.model.User;
import com.blueocean.auth.service.CountryValidationService;
import com.blueocean.auth.service.JwtBlacklistService;
import com.blueocean.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Core user service handling registration, OTP verification, login, and logout.
 * Users are stored in-memory using a ConcurrentHashMap (thread-safe).
 * Implements UserDetailsService for Spring Security integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserDetailsService {

    // ── In-Memory Stores ──────────────────────────────────────────────────────

    /** Primary user store: username → User */
    private final ConcurrentHashMap<String, User> userStore = new ConcurrentHashMap<>();

    /** Pending OTP store: username → OTP string (cleared after verification) */
    private final ConcurrentHashMap<String, String> otpStore = new ConcurrentHashMap<>();

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final PasswordEncoder passwordEncoder;
    private final CountryValidationService countryValidationService;
    private final JwtUtil jwtUtil;
    private final JwtBlacklistService blacklistService;

    // ── Validation Patterns ───────────────────────────────────────────────────

    /**
     * Password pattern: 8–12 chars, ≥1 uppercase, ≥1 lowercase, ≥1 digit, ≥1 special char.
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&_#^()\\-+=])[A-Za-z\\d@$!%*?&_#^()\\-+=]{8,12}$"
    );

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Registers a new user after validating country, fields, and uniqueness.
     *
     * @param request   registration data
     * @param ipAddress client IP for country check
     * @return AuthResponse containing generated OTP (for dev testing)
     */
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        log.info("Registration attempt for username: {}, IP: {}", request.getUsername(), ipAddress);

        // 1a. Selected country must be valid and not restricted
        if (!countryValidationService.isValidCountryCode(request.getCountry())) {
            throw new IllegalArgumentException("Invalid country selected");
        }
        if (countryValidationService.isCountryCodeRestricted(request.getCountry())) {
            String countryName = countryValidationService.getCountryName(request.getCountry());
            log.warn("Registration blocked for selected country: {}", request.getCountry());
            throw new CountryNotAllowedException(countryName);
        }

        // 1b. Country restriction check based on the client's IP
        if (!countryValidationService.isCountryAllowed(ipAddress)) {
            String countryCode = countryValidationService.getCountryCode(ipAddress);
            log.warn("Registration blocked for IP {} (country: {})", ipAddress, countryCode);
            throw new CountryNotAllowedException(countryCode);
        }

        // 2. Validate email (must contain '@' and '.')
        validateEmail(request.getEmail());

        // 3. Validate password complexity
        validatePassword(request.getPassword());

        // 4. Check username uniqueness
        if (userStore.containsKey(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + request.getUsername());
        }

        // 5. Check email uniqueness
        boolean emailExists = userStore.values().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(request.getEmail()));
        if (emailExists) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        // 6. Generate OTP (6-digit)
        String otp = generateOtp();
        otpStore.put(request.getUsername(), otp);
        log.info("OTP generated for {}: {} (in production this would be emailed)", request.getUsername(), otp);

        // 7. Build and store user (password hashed with BCrypt)
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(request.getUserType())
                .otpVerified(false)
                .enabled(false)  // enabled after OTP verification
                .build();

        userStore.put(user.getUsername(), user);
        log.info("User registered successfully: {}", user.getUsername());

        return AuthResponse.builder()
                .message("Registration successful. Please verify your OTP.")
                .otp(otp) // Return OTP in response for testing (remove in production)
                .build();
    }

    /**
     * Verifies OTP submitted by the user after registration.
     * Enables the user account on successful verification.
     *
     * @param username the registered username
     * @param otp      the OTP submitted by the user
     */
    public void verifyOtp(String username, String otp) {
        log.info("OTP verification attempt for: {}", username);

        if (!userStore.containsKey(username)) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        String storedOtp = otpStore.get(username);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new InvalidOtpException("Invalid or expired OTP");
        }

        // Activate user account
        User user = userStore.get(username);
        user.setOtpVerified(true);
        user.setEnabled(true);
        userStore.put(username, user);

        // Remove OTP from store (one-time use)
        otpStore.remove(username);
        log.info("OTP verified successfully for: {}", username);
    }

    /**
     * Invalidates the user's JWT token (logout).
     * After this, the token is rejected by JwtAuthFilter.
     *
     * @param token the JWT token to blacklist
     * @return username extracted from the token
     */
    public String logout(String token) {
        String username = jwtUtil.extractUsername(token);
        blacklistService.blacklist(token);
        log.info("User logged out: {}", username);
        return username;
    }

    /**
     * Loads a user by username for Spring Security authentication.
     * Throws UsernameNotFoundException if not found (triggers "Login Failed").
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userStore.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return user;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Validates that the email contains both '@' and '.'.
     */
    private void validateEmail(String email) {
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Email must contain '@' and '.'");
        }
    }

    /**
     * Validates the password against the complexity pattern:
     * 8–12 characters, ≥1 uppercase, ≥1 lowercase, ≥1 digit, ≥1 special character.
     */
    private void validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must be 8–12 characters and contain at least 1 uppercase letter, " +
                    "1 lowercase letter, 1 digit, and 1 special character (@$!%*?&_#^()-+=)"
            );
        }
    }

    /**
     * Generates a random 6-digit OTP.
     */
    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}
