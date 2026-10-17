package com.blueocean.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user login.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request payload")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Registered username", example = "johnsmith")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "User's password", example = "Pass@1234")
    private String password;
}
