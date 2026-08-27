package com.carebridge.backend.identity.admin.entity;

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
@Table(name = "account_lock_appeals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLockAppeal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "appeal_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lock_episode_id", nullable = false)
    private UUID lockEpisodeId;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccountLockAppealStatus status;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;
}

