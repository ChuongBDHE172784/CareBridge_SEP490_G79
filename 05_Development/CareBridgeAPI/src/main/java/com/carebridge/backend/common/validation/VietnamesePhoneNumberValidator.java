package com.carebridge.backend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class VietnamesePhoneNumberValidator implements ConstraintValidator<VietnamesePhoneNumber, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return VietnamesePhoneNumbers.isValidOrBlank(value);
    }
}
