package com.carebridge.backend.file.repository;

import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FilePurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {

    long countByOwnerUserIdAndStatus(UUID ownerUserId, FileStatus status);

    List<UploadedFile> findAllByIdInAndOwnerUserIdAndStatus(List<UUID> ids, UUID ownerUserId, FileStatus status);

    Optional<UploadedFile> findByIdAndStatus(UUID id, FileStatus status);

    // ContentImageOrphanCleanup_TDS.md ADR-CLEAN-001: purpose alone is NOT a safe filter — see
    // ADR-CLEAN-001 phương án A0 (bị loại). accessMode=PUBLIC is required to exclude images
    // uploaded through generic upload paths that happen to default to the same purpose label.
    List<UploadedFile> findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
            FilePurpose purpose, FileAccessMode accessMode, FileStatus status, Instant cutoff);
}
