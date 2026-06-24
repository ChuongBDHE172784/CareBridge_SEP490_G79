package com.carebridge.backend.consultation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "consultation_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "sender_user_id")
    private UUID senderUserId;

    @Column(name = "message_type", length = 20)
    private String messageType;

    @Column(name = "message_body", columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
