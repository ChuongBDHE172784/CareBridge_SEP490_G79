package com.carebridge.backend.community.exception;

import com.carebridge.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** UC-20 (CB-COMMUNITY-IMP-020 §10 — COMM-001). */
public class CommunityProfileAlreadyExistsException extends BusinessException {

    public CommunityProfileAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, "COMM-001", message);
    }
}
