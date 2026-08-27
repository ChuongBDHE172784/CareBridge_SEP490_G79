package com.carebridge.backend.directchat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "direct_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "message_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "sender_user_id", nullable = false, updatable = false)
    private UUID senderUserId;

    @Column(name = "client_message_id", nullable = false, updatable = false)
    private UUID clientMessageId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "message_body", columnDefinition = "text")
    private String messageBody;

    @Column(name = "attachment_id")
    private UUID attachmentId;

    @Column(name = "location_latitude")
    private Double locationLatitude;

    @Column(name = "location_longitude")
    private Double locationLongitude;

    @Column(name = "location_label", length = 200)
    private String locationLabel;

    @Column(name = "recalled_at")
    private Instant recalledAt;

    @Column(name = "recalled_by_user_id")
    private UUID recalledByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
