package com.carebridge.backend.directchat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "archived_realtime_records")
@org.hibernate.annotations.SQLRestriction("legacy_table = 'conversation_calls'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationCall implements Persistable<UUID> {

    @Id
    @Column(name = "archive_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "initiated_by_user_id", nullable = false, updatable = false)
    private UUID initiatedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 10)
    private CallType callType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "call_status", nullable = false, length = 20)
    private CallStatus callStatus = CallStatus.INITIATED;

    @Column(name = "zego_room_id", nullable = false, length = 255)
    private String zegoRoomId;

    @Column(name = "initiated_at", nullable = false, updatable = false)
    private Instant initiatedAt;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "original_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(name = "legacy_table", nullable = false, updatable = false)
    private String legacyTable = "conversation_calls";

    @Column(name = "legacy_id", nullable = false, updatable = false)
    private String legacyId;

    @Transient
    @Builder.Default
    private boolean newEntity = true;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PrePersist
    void prepareArchiveIdentity() {
        legacyTable = "conversation_calls";
        legacyId = id.toString();
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        newEntity = false;
    }
}
