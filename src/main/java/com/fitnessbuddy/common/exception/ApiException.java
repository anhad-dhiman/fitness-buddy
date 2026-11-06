package com.fitnessbuddy.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected, business-level failures ("email already taken",
 * "wrong password"). Services throw these; they know nothing about HTTP
 * responses. {@link GlobalExceptionHandler} turns them into a consistent
 * JSON error body with the status carried here.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
