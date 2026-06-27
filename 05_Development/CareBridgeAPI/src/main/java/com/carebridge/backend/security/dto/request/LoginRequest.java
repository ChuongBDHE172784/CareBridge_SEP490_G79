package com.carebridge.backend.security.dto.request;

import com.carebridge.backend.common.validation.VietnamesePhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @VietnamesePhoneNumber
    private String phone;

    @Email
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @AssertTrue(message = "Either phone or email must be provided")
    private boolean isIdentifierPresent() {
        return (phone != null && !phone.isBlank()) || (email != null && !email.isBlank());
    }

    @AssertTrue(message = "Only one of phone or email must be provided")
    private boolean isExactlyOneIdentifier() {
        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        return hasPhone ^ hasEmail; // XOR - exactly one
    }
}
