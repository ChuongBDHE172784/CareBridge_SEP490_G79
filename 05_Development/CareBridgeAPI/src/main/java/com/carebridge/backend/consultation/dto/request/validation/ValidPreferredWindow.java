package com.carebridge.backend.consultation.dto.request.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PreferredWindowValidator.class)
public @interface ValidPreferredWindow {
    String message() default "preferredWindowStart and preferredWindowEnd must both be absent or form an increasing window";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
