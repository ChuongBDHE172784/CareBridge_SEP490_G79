package com.carebridge.backend.community.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_interactions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_topic_follow",
        columnNames = {"actor_user_id", "interaction_type", "topic_id"}
    ),
    indexes = {
        @Index(name = "community_interactions_actor_ix", columnList = "actor_user_id"),
        @Index(name = "community_interactions_topic_ix", columnList = "topic_id")
    }
)
@org.hibernate.annotations.SQLRestriction("interaction_type = 'FOLLOW'")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserTopicFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "interaction_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "actor_user_id", nullable = false)
    private UUID userId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Builder.Default @Column(name = "interaction_type", nullable = false, updatable = false)
    private String interactionType = "FOLLOW";
}
