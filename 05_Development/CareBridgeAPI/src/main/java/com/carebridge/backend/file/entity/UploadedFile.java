package com.carebridge.backend.file.entity;

import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.enums.FilePurpose;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attachment_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Builder.Default
    @Column(name = "uploader_role", nullable = false, length = 30)
    private String uploaderRole = "PATIENT";

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Transient
    @Builder.Default
    private String storageProvider = "cloudinary";

    @Transient
    @Builder.Default
    private FileKind kind = FileKind.IMAGE;

    @Transient
    private FilePurpose purpose;

    @Transient
    @Builder.Default
    private FileAccessMode accessMode = FileAccessMode.PRIVATE;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "file_url", columnDefinition = "text")
    private String fileUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FileStatus status = FileStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
