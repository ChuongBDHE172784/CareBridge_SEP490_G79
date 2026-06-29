package com.carebridge.backend.community.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_bookmarks", indexes = {
    @Index(name = "idx_community_bookmarks_user", columnList = "user_id"),
    @Index(name = "idx_community_bookmarks_question", columnList = "question_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunityBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
