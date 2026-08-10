package com.lorafilm.booking.common.validation;

import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSeatReservation {
    String message() default "Invalid seat reservation payload";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
