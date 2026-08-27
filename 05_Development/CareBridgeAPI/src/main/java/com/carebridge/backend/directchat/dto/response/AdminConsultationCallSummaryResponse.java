package com.carebridge.backend.directchat.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminConsultationCallSummaryResponse {
    private final UUID callId;
    private final UUID conversationId;
    private final String callType;
    private final String callStatus;
    private final Instant initiatedAt;
    private final Instant answeredAt;
    private final Instant endedAt;
    private final Integer durationSeconds;
    private final UUID recordingFileId;
    private final String recordingStatus;
    private final Integer recordedDurationSeconds;
    private final Boolean consentAttested;

    // Caller & Participants detail
    private final UUID initiatedByUserId;
    private final String initiatedByRole;

    private final UUID motherUserId;
    private final String motherName;
    private final String motherPhone;
    private final String motherEmail;

    private final UUID expertUserId;
    private final String expertName;
    private final String expertSpecialization;
    private final String expertHospital;
}
