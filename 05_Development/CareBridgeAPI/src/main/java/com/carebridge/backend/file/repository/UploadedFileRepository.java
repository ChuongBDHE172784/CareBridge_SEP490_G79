package com.carebridge.backend.file.repository;

import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {

    long countByOwnerUserIdAndStatus(UUID ownerUserId, FileStatus status);

    List<UploadedFile> findAllByIdInAndOwnerUserIdAndStatus(List<UUID> ids, UUID ownerUserId, FileStatus status);

    Optional<UploadedFile> findByIdAndStatus(UUID id, FileStatus status);
}
