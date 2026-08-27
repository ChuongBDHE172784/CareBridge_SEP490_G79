package com.carebridge.backend.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "community_interactions",
        uniqueConstraints = @UniqueConstraint(
                name = "question_notification_mutes_user_question_unique",
                columnNames = {"actor_user_id", "interaction_type", "content_id"}))
@org.hibernate.annotations.SQLRestriction("interaction_type = 'MUTE' AND target_content_type = 'QUESTION'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionNotificationMute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "interaction_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "actor_user_id", nullable = false)
    private UUID userId;

    @Column(name = "content_id", nullable = false)
    private UUID questionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(name = "interaction_type", nullable = false, updatable = false)
    private String interactionType = "MUTE";

    @Builder.Default
    @Column(name = "target_content_type", nullable = false, updatable = false)
    private String targetContentType = "QUESTION";
}
