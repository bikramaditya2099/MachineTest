package com.blueocean.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response wrapper used across all endpoints.
 * Provides consistent structure: success flag, message, and optional data payload.
 *
 * @param <T> type of the data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Whether the request succeeded", example = "true")
    private boolean success;

    @Schema(description = "Human-readable response message")
    private String message;

    @Schema(description = "Response data payload (may be null for error responses)")
    private T data;

    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates a successful response without data.
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Creates an error response.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
