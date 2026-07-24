package com.carebridge.backend.directchat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@org.hibernate.annotations.SQLRestriction("legacy_table = 'direct_conversations'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "archive_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mother_user_id", nullable = false, updatable = false)
    private UUID motherUserId;

    @Column(name = "expert_user_id", nullable = false, updatable = false)
    private UUID expertUserId;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "original_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "mother_last_read_at")
    private Instant motherLastReadAt;

    @Column(name = "mother_last_read_message_id")
    private UUID motherLastReadMessageId;

    @Column(name = "expert_last_read_at")
    private Instant expertLastReadAt;

    @Column(name = "expert_last_read_message_id")
    private UUID expertLastReadMessageId;

    @Builder.Default
    @Column(name = "legacy_table", nullable = false, updatable = false)
    private String legacyTable = "direct_conversations";

    @Column(name = "legacy_id", nullable = false, updatable = false)
    private String legacyId;

    @PrePersist
    void prepareArchiveIdentity() {
        legacyTable = "direct_conversations";
        legacyId = id.toString();
    }
}
