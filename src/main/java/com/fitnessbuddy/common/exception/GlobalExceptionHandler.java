package com.fitnessbuddy.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Turns every exception thrown by a controller into an RFC 9457
 * "Problem Details" JSON body ({@code application/problem+json}):
 *
 * <pre>
 * { "type": "about:blank", "title": "Conflict", "status": 409,
 *   "detail": "An account with this email already exists", "instance": "/auth/register" }
 * </pre>
 *
 * Extending {@link ResponseEntityExceptionHandler} gives correct Problem
 * Details for Spring MVC's own exceptions for free: malformed JSON (400),
 * unsupported media type (415), wrong HTTP method (405), unknown path (404),
 * {@code ResponseStatusException}, and so on.
 *
 * Note: exceptions thrown inside the security filter chain (before a
 * controller is reached) do not pass through here. Those are configured in
 * SecurityConfig.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Expected business failures thrown by services. */
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }

    /**
     * A database constraint rejected the write. Services translate the cases
     * they anticipate (e.g. duplicate email) into a precise ConflictException;
     * this is the safety net for anything else. The raw SQL error is logged
     * but never returned, because it leaks table and constraint names.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The request conflicts with existing data");
    }

    /** Bean Validation failures on {@code @Valid @RequestBody}: 400 plus per-field messages. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, List<String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        TreeMap::new,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())));

        ProblemDetail body = ex.getBody();
        body.setDetail("Request validation failed");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Last resort: a bug. Log the full stack trace for us, return a generic
     * 500 to the client. Never send internals (SQL, stack traces) to the client.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) throws Exception {
        // Let Spring Security handle its own exceptions (403/401) if they are
        // thrown from inside a controller, instead of reporting them as 500.
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }
}
