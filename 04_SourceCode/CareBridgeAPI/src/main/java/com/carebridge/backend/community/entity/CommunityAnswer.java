package com.carebridge.backend.community.entity;

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
@Table(name = "community_answers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "answer_id")
    private UUID answerId;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "author_user_id")
    private UUID authorUserId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "answer_type", length = 20)
    private String answerType;

    @Column(name = "expert_profile_id")
    private UUID expertProfileId;

    @Column(name = "moderation_status", length = 20)
    private String moderationStatus;

    @Column(name = "helpful_count")
    private Integer helpfulCount;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
