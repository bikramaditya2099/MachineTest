package com.blueocean.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for Product management stored in PostgreSQL.
 * Supports full CRUD operations with pagination and filtering.
 */
@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    /** Auto-generated primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Product name (required) */
    @Column(nullable = false, length = 100)
    private String name;

    /** Product description */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Product price */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Available stock quantity */
    @Column(nullable = false)
    private Integer quantity;

    /** Timestamp when the product was created */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the product was last updated */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Automatically set createdAt before persisting */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /** Automatically update updatedAt before each update */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
