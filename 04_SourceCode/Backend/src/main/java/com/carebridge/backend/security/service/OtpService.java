package com.carebridge.backend.security.service;

import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.rbac.Role;

public interface OtpService {

    OtpVerification createAndSend(
            String phone,
            OtpVerification.OtpPurpose purpose,
            String email,
            Role requestedRole);

    OtpVerification verify(String phone, String otp);
}
