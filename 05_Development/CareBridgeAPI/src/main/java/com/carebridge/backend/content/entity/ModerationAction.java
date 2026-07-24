package com.carebridge.backend.content.entity;

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
@Table(name = "moderation_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "moderation_event_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "moderation_case_id", columnDefinition = "uuid")
    private UUID reportId;

    @Column(name = "target_id", columnDefinition = "uuid")
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30)
    private ReportTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 30)
    private ModerationActionType actionType;

    @Column(name = "moderator_user_id", columnDefinition = "uuid")
    private UUID moderatorUserId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "action_at")
    private Instant actionAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
