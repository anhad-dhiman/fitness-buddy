package com.fitnessbuddy.common.exception;

import org.springframework.http.HttpStatus;

/** 409: the request is valid but clashes with existing data (e.g. duplicate email). */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
