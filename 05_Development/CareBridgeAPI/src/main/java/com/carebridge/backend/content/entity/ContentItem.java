package com.carebridge.backend.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    @Column(name = "content_item_id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 30)
    private ContentType type;

    @Column(name = "title", length = 250)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 30)
    private ContentStage stage;

    @Column(name = "topic_id")
    private UUID topicId;

    /**
     * Tags reuse the existing content_item_topics join table.  Unlike topicId (the single
     * content category), this collection only contains CommunityTopic records of type TAG.
     */
    @ElementCollection
    @CollectionTable(name = "content_item_topics", joinColumns = @JoinColumn(name = "content_item_id"))
    @Column(name = "topic_id")
    @Builder.Default
    private List<UUID> tagIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContentStatus status;

    @Column(name = "version_no")
    private Integer versionNo;

    @Column(name = "author_user_id")
    private UUID authorUserId;

    @Column(name = "revision_reason", columnDefinition = "TEXT")
    private String revisionReason;

    @Column(name = "revision_requested_at")
    private Instant revisionRequestedAt;

    @Column(name = "revision_requested_by")
    private UUID revisionRequestedBy;

    @Column(name = "revision_requested_version")
    private Integer revisionRequestedVersion;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "source_label", length = 255)
    private String sourceLabel;

    @ElementCollection
    @CollectionTable(name = "content_item_sources", joinColumns = @JoinColumn(name = "content_item_id"))
    @Builder.Default
    private List<ContentSource> sources = new ArrayList<>();

    @Column(name = "published_at")
    private Instant publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
