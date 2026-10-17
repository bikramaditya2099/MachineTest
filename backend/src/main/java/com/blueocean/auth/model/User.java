package com.blueocean.auth.model;

import com.blueocean.auth.model.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * In-memory User model implementing Spring Security's UserDetails.
 * Users are stored in a ConcurrentHashMap (no DB persistence for users).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    /** Unique identifier for the user */
    private String id;

    /** Username (8–50 characters) */
    private String username;

    /** User's email address (must contain '@' and '.') */
    private String email;

    /** BCrypt-hashed password */
    private String password;

    /** Role of the user: STANDARD_USER or ADMINISTRATOR */
    private UserType userType;

    /** Whether OTP has been verified */
    private boolean otpVerified;

    /** Account enabled flag */
    private boolean enabled;

    /**
     * Returns granted authority based on the user's type.
     * Spring Security requires "ROLE_" prefix for role-based checks.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userType.name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
