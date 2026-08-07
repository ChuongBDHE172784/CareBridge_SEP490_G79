package com.carebridge.backend.directchat.entity;

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
@Table(name = "direct_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "conversation_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mother_user_id", nullable = false, updatable = false)
    private UUID motherUserId;

    @Column(name = "expert_user_id", nullable = false, updatable = false)
    private UUID expertUserId;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    // Read state lives in direct_conversation_read_cursors, keyed by
    // (conversation_id, reader_user_id). The four per-role columns that used to sit
    // here were dropped: they could not represent a family member reading the
    // thread, and a second writable copy of the same state only invited drift.
}
