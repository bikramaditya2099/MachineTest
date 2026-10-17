package com.blueocean.auth.dto;

import com.blueocean.auth.model.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user registration.
 * Contains all validation constraints as per requirements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request payload")
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 8, max = 50, message = "Username must be between 8 and 50 characters")
    @Schema(description = "Username (8–50 characters)", example = "johnsmith")
    private String username;

    @NotBlank(message = "Email is required")
    @Schema(description = "Email address (must contain '@' and '.')", example = "john.smith@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(
        description = "Password: 8–12 chars, ≥1 uppercase, ≥1 lowercase, ≥1 digit, ≥1 special character",
        example = "Pass@1234"
    )
    private String password;

    @NotNull(message = "User type is required")
    @Schema(description = "User role: STANDARD_USER or ADMINISTRATOR", example = "STANDARD_USER")
    private UserType userType;

    @NotBlank(message = "Country is required")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country must be a 2-letter ISO code")
    @Schema(description = "Country of the user as ISO 3166-1 alpha-2 code (restricted countries are blocked)", example = "IN")
    private String country;
}
