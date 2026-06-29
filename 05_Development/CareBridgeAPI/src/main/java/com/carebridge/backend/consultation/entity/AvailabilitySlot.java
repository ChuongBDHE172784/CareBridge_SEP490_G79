package com.carebridge.backend.consultation.entity;

import com.carebridge.backend.expert.enums.SlotStatus;
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
 * Availability slot entity.
 * Represents an available time slot that an expert has configured for consultations.
 *
 * ADR-EXP-002: Separate table for efficient conflict detection.
 */
@Entity
@Table(name = "expert_availability")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "availability_id")
    private Long availabilityId;

    /**
     * Foreign key to experts table.
     */
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    /**
     * Start time of the availability slot.
     * ERD: start_at
     */
    @Column(name = "start_at", nullable = false)
    private Instant slotStart;

    /**
     * End time of the availability slot.
     * Must be greater than slot_start.
     * ERD: end_at
     */
    @Column(name = "end_at", nullable = false)
    private Instant slotEnd;

    /**
     * Consultation modality for this slot.
     * CHAT, VOICE, or VIDEO.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 50)
    private String channelType;

    /**
     * Current status of the slot.
     * AVAILABLE, BOOKED, BLOCKED, UNAVAILABLE.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SlotStatus status;

    /**
     * If booked, references the consultation booking this slot is allocated to.
     */
    @Column(name = "booking_id")
    private Long bookingId;

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
