package com.carebridge.backend.file.repository;

import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FilePurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {

    long countByOwnerUserIdAndStatus(UUID ownerUserId, FileStatus status);

    List<UploadedFile> findAllByIdInAndOwnerUserIdAndStatus(List<UUID> ids, UUID ownerUserId, FileStatus status);

    Optional<UploadedFile> findByIdAndStatus(UUID id, FileStatus status);

    List<UploadedFile> findAllByOwnerUserIdAndFileUrlInAndStatus(
            UUID ownerUserId, java.util.Collection<String> fileUrls, FileStatus status);

    // ContentImageOrphanCleanup_TDS.md ADR-CLEAN-001: purpose alone is NOT a safe filter — see
    // ADR-CLEAN-001 phương án A0 (bị loại). accessMode=PUBLIC is required to exclude images
    // uploaded through generic upload paths that happen to default to the same purpose label.
    // Canonical schema note: UploadedFile.purpose/accessMode are @Transient — purpose maps to
    // attachments.attachment_category; access_mode has no canonical column, so the accessMode
    // guard cannot be expressed here (kept as an unused parameter to preserve the call contract).
    @Query(value = """
            SELECT * FROM attachments
             WHERE attachment_category = :#{#purpose.name()}
               AND status = :#{#status.name()}
               AND created_at < :cutoff
            """, nativeQuery = true)
    List<UploadedFile> findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
            @Param("purpose") FilePurpose purpose, @Param("accessMode") FileAccessMode accessMode,
            @Param("status") FileStatus status, @Param("cutoff") Instant cutoff);
}
