package com.blueocean.auth.exception;

/** Thrown when an invalid or expired OTP is submitted. */
public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String message) {
        super(message);
    }
}
