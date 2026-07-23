package com.carebridge.backend.community.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_content", indexes = {
    @Index(name = "community_content_topic_ix", columnList = "topic_id"),
    @Index(name = "community_content_author_ix", columnList = "author_user_id"),
    @Index(name = "community_content_status_ix", columnList = "moderation_status"),
    @Index(name = "community_content_stage_ix", columnList = "stage")
})
@org.hibernate.annotations.SQLRestriction("content_type = 'QUESTION'")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunityQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "content_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "topic_id")
    private UUID topicId;

    @Column(name = "author_user_id", nullable = false)
    private java.util.UUID authorId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 30)
    private PregnancyStage stage;

    @Column(name = "pregnancy_week")
    private Short pregnancyWeek;

    @Column(name = "baby_age_months")
    private Short babyAgeMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", length = 20)
    private UrgencyLevel urgency;

    @Column(name = "is_anonymous", nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private boolean anonymous = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 30)
    @Builder.Default
    private QuestionStatus status = QuestionStatus.PENDING;

    @Column(name = "like_count", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private int likeCount = 0;

    @Column(name = "answer_count", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private int answerCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "content_type", nullable = false, updatable = false, length = 20)
    private String contentType = "QUESTION";
}
