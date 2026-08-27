package com.carebridge.backend.community.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_interactions", indexes = {
    @Index(name = "community_interactions_actor_ix", columnList = "actor_user_id"),
    @Index(name = "community_interactions_content_ix", columnList = "content_id")
})
@org.hibernate.annotations.SQLRestriction("interaction_type = 'BOOKMARK' AND target_content_type = 'QUESTION'")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunityBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "interaction_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "actor_user_id", nullable = false)
    private UUID userId;

    @Column(name = "content_id", nullable = false)
    private UUID questionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Builder.Default @Column(name = "interaction_type", nullable = false, updatable = false)
    private String interactionType = "BOOKMARK";

    @Builder.Default @Column(name = "target_content_type", nullable = false, updatable = false)
    private String targetContentType = "QUESTION";
}
