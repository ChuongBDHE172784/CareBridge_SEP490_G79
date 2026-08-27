package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ReportStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** UC-14 Report Content or Account (CB-MOD-IMP-014 §8.1). */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportResponse {

    private UUID reportId;
    private ReportStatus status;
    private Instant createdAt;
}
