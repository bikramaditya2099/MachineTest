package com.blueocean.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for OTP verification after registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OTP verification request payload")
public class OtpVerifyRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username from registration", example = "johnsmith")
    private String username;

    @NotBlank(message = "OTP is required")
    @Schema(description = "6-digit OTP received after registration", example = "482910")
    private String otp;
}
