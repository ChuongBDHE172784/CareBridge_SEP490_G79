package com.carebridge.backend.content.entity;

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
@Table(name = "moderation_actions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "moderation_action_id")
    private UUID moderationActionId;

    @Column(name = "report_id")
    private UUID reportId;

    @Column(name = "moderator_user_id")
    private UUID moderatorUserId;

    @Column(name = "target_type", length = 30)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "action_type", length = 30)
    private String actionType;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "action_at")
    private Instant actionAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
