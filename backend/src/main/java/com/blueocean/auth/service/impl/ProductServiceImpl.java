package com.blueocean.auth.service.impl;

import com.blueocean.auth.dto.ProductRequest;
import com.blueocean.auth.exception.ResourceNotFoundException;
import com.blueocean.auth.model.Product;
import com.blueocean.auth.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Service layer for product CRUD operations.
 * Supports pagination, sorting, and name-based filtering.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl {

    private final ProductRepository productRepository;

    /**
     * Retrieves a paginated, sorted list of products with optional name filtering.
     *
     * @param page      zero-indexed page number (default: 0)
     * @param size      number of items per page (default: 10)
     * @param sortBy    field to sort by (default: "id")
     * @param sortDir   sort direction: "asc" or "desc" (default: "asc")
     * @param nameFilter optional partial name filter (empty = return all)
     * @return Page of Product entities
     */
    public Page<Product> getAllProducts(int page, int size, String sortBy,
                                        String sortDir, String nameFilter) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        if (nameFilter != null && !nameFilter.isBlank()) {
            return productRepository.findByNameContainingIgnoreCase(nameFilter, pageable);
        }

        return productRepository.findAll(pageable);
    }

    /**
     * Retrieves a single product by its ID.
     *
     * @param id product ID
     * @return Product entity
     * @throws ResourceNotFoundException if product not found
     */
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    /**
     * Creates a new product from the request DTO.
     *
     * @param request product creation data
     * @return saved Product entity
     */
    public Product createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created with id: {}", saved.getId());
        return saved;
    }

    /**
     * Updates an existing product.
     *
     * @param id      product ID to update
     * @param request updated product data
     * @return updated Product entity
     * @throws ResourceNotFoundException if product not found
     */
    public Product updateProduct(Long id, ProductRequest request) {
        Product product = getProductById(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());

        Product updated = productRepository.save(product);
        log.info("Product updated with id: {}", id);
        return updated;
    }

    /**
     * Deletes a product by its ID.
     *
     * @param id product ID to delete
     * @throws ResourceNotFoundException if product not found
     */
    public void deleteProduct(Long id) {
        Product product = getProductById(id); // Throws if not found
        productRepository.delete(product);
        log.info("Product deleted with id: {}", id);
    }
}
