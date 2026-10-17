package com.blueocean.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a product.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product create/update request payload")
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name cannot exceed 100 characters")
    @Schema(description = "Name of the product", example = "Wireless Mouse")
    private String name;

    @Schema(description = "Product description", example = "Ergonomic wireless mouse with USB receiver")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer and 2 decimal digits")
    @Schema(description = "Product price", example = "29.99")
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Schema(description = "Available stock quantity", example = "100")
    private Integer quantity;
}
