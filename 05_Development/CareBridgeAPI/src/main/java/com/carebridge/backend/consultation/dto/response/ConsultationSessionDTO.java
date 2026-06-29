package com.carebridge.backend.consultation.dto.response;

import com.carebridge.backend.expert.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Consultation session response DTO.
 * Maps to consultation_sessions entity with booking_id FK.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationSessionDTO {

    private Long sessionId;
    private Long bookingId; // FK to consultation_bookings.booking_id
    private String communicationRoomId;
    private String sessionToken;
    private String providerType;
    private SessionStatus sessionStatus;
    private String expertSummary;
    private Instant startedAt;
    private Instant endedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
