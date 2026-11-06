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
import org.springframework.security.web.csrf.CsrfException;
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
 * Security errors (401/403) are raised in filters, before any controller, and
 * would normally bypass this class. SecurityConfig forwards them here, so they
 * get the same format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Not authenticated: no token, or an invalid/expired one. Forwarded by
     * SecurityConfig's authenticationEntryPoint.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource");
    }

    /**
     * Authenticated but not allowed, or a missing/wrong CSRF token. Forwarded by
     * SecurityConfig's accessDeniedHandler (CSRF failures are AccessDeniedExceptions).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        String detail = ex instanceof CsrfException
                ? "Missing or invalid CSRF token. Send the XSRF-TOKEN cookie value in the X-XSRF-TOKEN header"
                : "You do not have permission to perform this action";
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
    }

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
     * Last resort: a bug. Log the full stack trace for us, return a generic 500
     * to the client. Never send internals (SQL, stack traces) to the client.
     *
     * Spring always picks the most specific matching handler, so the handlers
     * above (e.g. AccessDeniedException) win over this one for their types.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }
}
