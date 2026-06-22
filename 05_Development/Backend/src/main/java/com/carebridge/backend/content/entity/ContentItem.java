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
@Table(name = "content_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "content_item_id")
    private UUID contentItemId;

    @Column(name = "topic_id")
    private UUID topicId;

    @Column(name = "author_user_id")
    private UUID authorUserId;

    @Column(name = "content_type", length = 30)
    private String contentType;

    @Column(name = "title", length = 250)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "source_label", length = 255)
    private String sourceLabel;

    @Column(name = "version_no")
    private Integer versionNo;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
