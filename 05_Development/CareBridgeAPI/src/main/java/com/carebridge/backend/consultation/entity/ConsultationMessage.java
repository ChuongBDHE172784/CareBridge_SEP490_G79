package com.carebridge.backend.consultation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Consultation message entity.
 * Represents a message sent during a consultation session.
 *
 * Supports: TEXT, IMAGE, FILE, AUDIO, VIDEO, SYSTEM.
 */
@Entity
@Table(name = "consultation_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    /**
     * Foreign key to consultation_sessions table.
     */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /**
     * User who sent the message.
     */
    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    /**
     * Type of message.
     * TEXT, IMAGE, FILE, AUDIO, VIDEO, SYSTEM.
     */
    @Column(name = "message_type", nullable = false, length = 50)
    private String messageType;

    /**
     * Message content (text or description).
     * For file types, may contain caption/description.
     */
    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    /**
     * URL to attached file (if any).
     */
    @Column(name = "file_url")
    private String fileUrl;

    /**
     * When the message was sent.
     */
    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private Instant sentAt = Instant.now();

    /**
     * When the message was read by recipient.
     */
    @Column(name = "read_at")
    private Instant readAt;

    /**
     * Delivery status: SENT, DELIVERED, READ, FAILED.
     */
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "SENT";

    /**
     * Creation timestamp (for DB audit).
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
