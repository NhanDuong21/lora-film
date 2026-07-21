package com.lorafilm.booking.common.validation;

import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBookingCode {
    String message() default "Invalid booking code format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
