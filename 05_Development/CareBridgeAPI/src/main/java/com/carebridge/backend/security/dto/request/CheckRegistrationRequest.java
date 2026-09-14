package com.carebridge.backend.security.dto.request;

import com.carebridge.backend.common.validation.VietnamesePhoneNumber;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckRegistrationRequest {

    @Email
    private String email;

    @VietnamesePhoneNumber
    private String phone;
}
