package com.carebridge.backend.security.dto.request;

import com.carebridge.backend.common.validation.VietnamesePhoneNumber;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @VietnamesePhoneNumber
    private String phone;
}
