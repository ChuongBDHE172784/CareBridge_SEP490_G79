package com.carebridge.backend.security.dto.request;

import com.carebridge.backend.common.validation.VietnamesePhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {

    @VietnamesePhoneNumber
    private String phone;

    // Story 1.2 fix: setter trims the email so that leading/trailing whitespace
    // does not trip @Email validation before verifyOtp() can canonicalize the
    // value via trim().toLowerCase(). The empty-after-trim result still fails
    // @Email and @NotBlank, which is the correct behavior for a blank payload.
    @Email
    private String email;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "OTP must contain 6 digits")
    private String otp;
}
