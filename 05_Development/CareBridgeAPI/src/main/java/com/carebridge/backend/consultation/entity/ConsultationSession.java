package com.carebridge.backend.consultation.entity;

import com.carebridge.backend.expert.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Consultation session entity.
 * Represents a realtime communication session for a consultation.
 *
 * Maps to ERD: consultation_sessions (PK: session_id, FK: booking_id)
 *
 * Linked to external realtime provider (ZegoCloud, Firebase, etc.).
 */
@Entity
@Table(name = "consultation_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    /**
     * Foreign key to consultation_bookings table.
     * ERD: booking_id
     */
    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    /**
     * External provider's room/room ID.
     */
    @Column(name = "communication_room_id", nullable = false, length = 200)
    private String communicationRoomId;

    /**
     * Session token for client authentication.
     */
    @Column(name = "session_token", nullable = false)
    private String sessionToken;

    /**
     * Provider type (MOCK, ZEGOCLOUD, FIREBASE).
     */
    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType;

    /**
     * When the session was started.
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * When the session was ended.
     */
    @Column(name = "ended_at")
    private Instant endedAt;

    /**
     * Current session status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false, length = 50)
    private SessionStatus sessionStatus;

    /**
     * Expert's summary/notes after the session.
     */
    @Column(name = "expert_summary", columnDefinition = "TEXT")
    private String expertSummary;

    /**
     * Technical logs from the realtime provider (JSON).
     */
    @Column(name = "technical_log_json", columnDefinition = "JSONB")
    private String technicalLogJson;

    /**
     * Creation timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
