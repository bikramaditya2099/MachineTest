package com.blueocean.auth.repository;

import com.blueocean.auth.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Product entity.
 * Provides CRUD operations and pagination out of the box.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find products by name (case-insensitive, partial match) with pagination.
     * Used for filtering/search functionality.
     *
     * @param name     partial product name to search
     * @param pageable pagination and sort parameters
     * @return paginated list of matching products
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
