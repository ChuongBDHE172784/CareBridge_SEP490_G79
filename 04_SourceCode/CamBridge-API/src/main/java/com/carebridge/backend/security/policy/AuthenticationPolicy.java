package com.carebridge.backend.security.policy;

import com.carebridge.backend.common.exception.AuthenticationException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationPolicy {

    public void ensureCanAuthenticate(User user) {
        if (user == null || !user.isEnabled()) {
            throw new AuthenticationException("Account is disabled");
        }
        if (user.isLocked()) {
            throw new AuthenticationException("Account is locked");
        }
    }

    public Role resolveSelfRegistrationRole(Role requestedRole) {
        Role role = requestedRole == null ? Role.MOTHER : requestedRole;
        if (role == Role.MOTHER || role == Role.FAMILY || role == Role.EXPERT) {
            return role;
        }
        throw new ValidationException("Role is not allowed for self-registration");
    }

    public void ensureOtpCanBeAttempted(OtpVerification verification, int maxAttempts) {
        if (verification.getAttempts() >= maxAttempts) {
            throw new ValidationException("OTP attempt limit exceeded");
        }
    }
}
