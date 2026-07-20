package com.carebridge.backend.contribution.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contribution_attachments",
        indexes = {
                @Index(name = "idx_contrib_attachments_contribution_id", columnList = "contribution_id"),
                @Index(name = "idx_contrib_attachments_file_id", columnList = "file_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attachment_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "contribution_id", nullable = false)
    private UUID contributionId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private com.carebridge.backend.file.enums.FileKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 50)
    private com.carebridge.backend.file.enums.FilePurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false, length = 20)
    private com.carebridge.backend.file.enums.FileAccessMode accessMode;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}