package com.fitnessbuddy.common.exception;

import org.springframework.http.HttpStatus;

/** 401: the caller is not (or no longer) authenticated, e.g. wrong credentials. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
