package com.carebridge.backend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class VietnamesePhoneNumberValidator implements ConstraintValidator<VietnamesePhoneNumber, String> {

    private static final String PHONE_PATTERN = "^(\\+84|0)(3|5|7|8|9)\\d{8}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // null/blank is acceptable here; use @NotNull/@NotBlank separately if required
        }
        return value.matches(PHONE_PATTERN);
    }
}
