package com.blueocean.auth.exception;

/** Thrown when a user attempts to register from a restricted country. */
public class CountryNotAllowedException extends RuntimeException {
    public CountryNotAllowedException(String country) {
        super("Registration is not allowed from: " + country);
    }
}
