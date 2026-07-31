package com.carebridge.backend.security.exception;

import com.carebridge.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Generic registration conflict that deliberately does not reveal whether the
 * email address or phone number already belongs to an account.
 */
public class AccountAlreadyExistsException extends BusinessException {

    public static final String ERROR_CODE = "AUTH_ACCOUNT_EXISTS";

    public AccountAlreadyExistsException() {
        super(HttpStatus.CONFLICT, ERROR_CODE, "Account already exists");
    }
}
