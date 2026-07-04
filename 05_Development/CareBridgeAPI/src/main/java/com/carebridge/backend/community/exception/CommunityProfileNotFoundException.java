package com.carebridge.backend.community.exception;

import com.carebridge.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** UC-21 (CB-COMMUNITY-IMP-021 §10 — COMM-011). */
public class CommunityProfileNotFoundException extends BusinessException {

    public CommunityProfileNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "COMM-011", message);
    }
}
