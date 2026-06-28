package com.project.promotionservice.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DiscountConstraintValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DiscountConstraint {
    String message() default "Invalid discount configurations";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
