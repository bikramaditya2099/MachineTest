package com.blueocean.auth.dto;

import com.blueocean.auth.model.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after successful authentication (login/register).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response containing JWT token and user details")
public class AuthResponse {

    @Schema(description = "JWT Bearer token to use in subsequent requests")
    private String token;

    @Schema(description = "Response message (e.g., 'Welcome johnsmith')")
    private String message;

    @Schema(description = "User's role")
    private UserType userType;

    @Schema(description = "OTP for verification (only returned on registration in dev)")
    private String otp;
}
