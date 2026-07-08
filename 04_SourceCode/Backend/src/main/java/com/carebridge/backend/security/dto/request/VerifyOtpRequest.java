package com.carebridge.backend.security.dto.request;

import com.carebridge.backend.common.validation.VietnamesePhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {

    @VietnamesePhoneNumber
    private String phone;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "OTP must contain 6 digits")
    private String otp;
}
