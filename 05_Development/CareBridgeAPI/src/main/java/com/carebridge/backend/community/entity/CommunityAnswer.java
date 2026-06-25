package com.carebridge.backend.community.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_answers", indexes = {
    @Index(name = "idx_community_answers_question_id", columnList = "question_id"),
    @Index(name = "idx_community_answers_author_id", columnList = "author_id"),
    @Index(name = "idx_community_answers_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunityAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    // ADR-COM-004: authorId from JWT — matches users.id (UUID)
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    // ADR-COM-005: only set by Moderator/System, never from request body
    @Column(name = "is_expert_labeled", nullable = false)
    @Builder.Default
    private boolean expertLabeled = false;

    @Column(name = "is_personal_experience", nullable = false)
    private boolean personalExperience;

    // ADR-COM-006: always PENDING on creation
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AnswerStatus status = AnswerStatus.PENDING;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
