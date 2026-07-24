package com.carebridge.backend.directchat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "archived_realtime_records")
@org.hibernate.annotations.SQLRestriction("legacy_table = 'direct_messages'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "archive_id", updatable = false, nullable = false)
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

    @Column(name = "message_body", nullable = false, columnDefinition = "text")
    private String messageBody;

    @Column(name = "original_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(name = "legacy_table", nullable = false, updatable = false)
    private String legacyTable = "direct_messages";

    @Column(name = "legacy_id", nullable = false, updatable = false)
    private String legacyId;

    @PrePersist
    void prepareArchiveIdentity() {
        legacyTable = "direct_messages";
        legacyId = id.toString();
    }
}
