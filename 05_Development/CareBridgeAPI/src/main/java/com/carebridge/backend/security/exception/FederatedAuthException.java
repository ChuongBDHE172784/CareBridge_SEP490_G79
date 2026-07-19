package com.carebridge.backend.security.exception;

import com.carebridge.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class FederatedAuthException extends BusinessException {
    public FederatedAuthException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static FederatedAuthException invalidProof() {
        return new FederatedAuthException(HttpStatus.UNAUTHORIZED, "AUTH-FED-001", "Unable to authenticate");
    }

    public static FederatedAuthException unsupportedProvider() {
        return new FederatedAuthException(HttpStatus.BAD_REQUEST, "AUTH-FED-002", "Unsupported identity provider");
    }

    public static FederatedAuthException collision() {
        return new FederatedAuthException(HttpStatus.CONFLICT, "AUTH-FED-003", "Existing account requires verification");
    }

    public static FederatedAuthException unavailable() {
        return new FederatedAuthException(HttpStatus.SERVICE_UNAVAILABLE, "AUTH-FED-005", "Identity provider unavailable");
    }

    public static FederatedAuthException identityOwnedByAnotherUser() {
        return new FederatedAuthException(
                HttpStatus.CONFLICT, "AUTH-FED-006", "This Google account cannot be linked");
    }

    public static FederatedAuthException userAlreadyLinkedDifferentIdentity() {
        return new FederatedAuthException(
                HttpStatus.CONFLICT, "AUTH-FED-007", "A different Google account is already linked");
    }
}
