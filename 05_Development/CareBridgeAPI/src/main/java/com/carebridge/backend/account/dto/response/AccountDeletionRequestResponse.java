package com.carebridge.backend.account.dto.response;

import com.carebridge.backend.account.entity.DeletionStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionRequestResponse {

    private UUID id;
    private UUID userId;
    private DeletionStatus status;
    private String reason;
    private Instant requestedAt;
    private Instant scheduledFor;
    private Instant processedAt;
}
