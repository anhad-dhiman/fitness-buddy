package com.fitnessbuddy.common.exception;

import org.springframework.http.HttpStatus;

/** 400: the request is malformed in a way Bean Validation can't express. */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
