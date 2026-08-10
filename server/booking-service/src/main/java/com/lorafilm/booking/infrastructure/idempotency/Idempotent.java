package com.lorafilm.booking.infrastructure.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce idempotency on API endpoints.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /**
     * Expiration time of the idempotency key in seconds.
     * Default is 24 hours (86400 seconds).
     */
    long expireInSeconds() default 86400;
}
