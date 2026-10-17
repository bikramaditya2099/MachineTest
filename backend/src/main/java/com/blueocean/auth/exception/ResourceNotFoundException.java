package com.blueocean.auth.exception;

/** Thrown when a requested resource (e.g., product) is not found. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
}
