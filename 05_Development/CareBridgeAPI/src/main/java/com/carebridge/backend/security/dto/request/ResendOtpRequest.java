package com.carebridge.backend.security.dto.request;

import com.carebridge.backend.common.validation.VietnamesePhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendOtpRequest {

    /**
     * Phone number (E.164 format) - optional nếu có email
     */
    @VietnamesePhoneNumber
    private String phone;

    /**
     * Email address - optional nếu có phone
     */
    @Email
    private String email;

    /**
     * Validate: phải có ít nhất một trong phone hoặc email.
     */
    @AssertTrue(message = "Exactly one of phone or email must be provided")
    private boolean isIdentifierPresent() {
        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        return hasPhone ^ hasEmail;
    }
}
