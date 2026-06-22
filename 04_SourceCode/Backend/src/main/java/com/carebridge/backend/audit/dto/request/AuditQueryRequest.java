package com.carebridge.backend.audit.dto.request;

import com.carebridge.backend.audit.entity.AuditAction;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditQueryRequest {

    private Long userId;
    private AuditAction action;
    private Instant fromDate;
    private Instant toDate;
}
