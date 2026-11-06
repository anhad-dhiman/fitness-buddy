package com.fitnessbuddy.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated string must be at most {@link #value()} bytes when encoded as
 * UTF-8. {@code null} is considered valid (combine with {@code @NotBlank}).
 *
 * Exists because {@code @Size} counts UTF-16 chars, not bytes: 20 emoji are
 * 40 chars but 80 bytes. BCrypt only accepts up to 72 bytes, and
 * Spring Security throws IllegalArgumentException beyond that.
 */
@Documented
@Constraint(validatedBy = MaxBytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxBytes {

    int value();

    String message() default "must be at most {value} bytes";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
