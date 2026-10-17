package com.blueocean.auth.controller;

import com.blueocean.auth.dto.ApiResponse;
import com.blueocean.auth.dto.ProductRequest;
import com.blueocean.auth.model.Product;
import com.blueocean.auth.service.impl.ProductServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for product CRUD operations.
 * All endpoints require a valid JWT token (authenticated users).
 * Supports pagination, sorting, and filtering.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management - CRUD with pagination, sorting, and filtering")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductServiceImpl productService;

    /**
     * GET /api/products
     * Returns a paginated, sorted list of products with optional name filter.
     *
     * @param page       page number (0-indexed, default: 0)
     * @param size       items per page (default: 10)
     * @param sortBy     field to sort (default: "id")
     * @param sortDir    "asc" or "desc" (default: "asc")
     * @param nameFilter partial name filter (optional)
     */
    @GetMapping
    @Operation(
        summary = "List products",
        description = "Returns a paginated list of products. Supports filtering by name, sorting, and pagination."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<Page<Product>>> getAllProducts(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Filter by product name (partial match)") @RequestParam(required = false) String name
    ) {
        Page<Product> products = productService.getAllProducts(page, size, sortBy, sortDir, name);
        return ResponseEntity.ok(ApiResponse.success(
                "Products retrieved successfully (page " + page + " of " + products.getTotalPages() + ")",
                products
        ));
    }

    /**
     * GET /api/products/{id}
     * Returns a single product by its ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Returns a single product by its ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product found", product));
    }

    /**
     * POST /api/products
     * Creates a new product.
     */
    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new product. Requires authentication.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<ApiResponse<Product>> createProduct(@Valid @RequestBody ProductRequest request) {
        Product created = productService.createProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", created));
    }

    /**
     * PUT /api/products/{id}
     * Updates an existing product.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product by ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        Product updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updated));
    }

    /**
     * DELETE /api/products/{id}
     * Deletes a product by its ID.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Deletes a product by ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }
}
