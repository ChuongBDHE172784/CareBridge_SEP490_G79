package com.carebridge.backend.consultation.dto.request.validation;

import com.carebridge.backend.consultation.dto.request.CreateConsultationRequestRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PreferredWindowValidator
        implements ConstraintValidator<ValidPreferredWindow, CreateConsultationRequestRequest> {

    @Override
    public boolean isValid(CreateConsultationRequestRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.getPreferredWindowStart() == null
                && value.getPreferredWindowEnd() == null) {
            return true;
        }
        return value.getPreferredWindowStart() != null
                && value.getPreferredWindowEnd() != null
                && value.getPreferredWindowEnd().isAfter(value.getPreferredWindowStart());
    }
}
