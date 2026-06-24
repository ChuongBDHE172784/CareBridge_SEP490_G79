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
@Table(name = "community_topics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "topic_id")
    private UUID topicId;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "slug", length = 160)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
