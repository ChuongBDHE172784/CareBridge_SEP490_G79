package com.carebridge.backend.community.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_topic_follows",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_topic_follow",
        columnNames = {"user_id", "topic_id"}
    ),
    indexes = {
        @Index(name = "idx_user_topic_follows_user",  columnList = "user_id"),
        @Index(name = "idx_user_topic_follows_topic", columnList = "topic_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserTopicFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
