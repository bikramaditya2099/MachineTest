package com.blueocean.auth.exception;

/** Thrown when trying to register a username or email that already exists. */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
