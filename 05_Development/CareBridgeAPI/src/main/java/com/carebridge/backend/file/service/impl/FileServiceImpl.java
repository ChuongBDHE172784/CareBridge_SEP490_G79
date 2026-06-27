package com.carebridge.backend.file.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IFileService;
import com.carebridge.backend.file.service.IStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    private static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;
    private static final long MAX_FILES_PER_ACCOUNT = 500L;
    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/png", "image/heic", "application/pdf", "image/gif");

    private final UploadedFileRepository fileRepository;
    private final IStorageService storageService;
    private final AuditService auditService;

    @Override
    public UploadFileResponse uploadFile(MultipartFile file, UUID callerId) {
        // C5: size check (FILE-002)
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(HttpStatus.CONTENT_TOO_LARGE, "FILE-002",
                    "File exceeds maximum size of 20MB");
        }

        // C4: storage quota check BEFORE writing to storage
        long existingCount = fileRepository.countByOwnerUserIdAndStatus(callerId, FileStatus.ACTIVE);
        if (existingCount >= MAX_FILES_PER_ACCOUNT) {
            throw new BusinessException(HttpStatus.CONFLICT, "FILE-003",
                    "Storage quota of 500 files exceeded");
        }

        // C1: validate MIME from magic bytes (not extension)
        String mimeType = detectMimeType(file);
        if (!ALLOWED_MIME.contains(mimeType)) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE-001",
                    "Unsupported file type: " + mimeType);
        }

        // C2: storageKey must be UUID-based (ADR-FILE-003)
        String fileExt = getExtension(file.getOriginalFilename());
        String storageKey = "files/" + UUID.randomUUID() + fileExt;

        try {
            storageService.store(storageKey, file.getBytes(), mimeType);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-004",
                    "Failed to store file");
        }

        // C4: accountId from JWT
        UploadedFile saved = fileRepository.save(UploadedFile.builder()
                .ownerUserId(callerId)
                .storageKey(storageKey)
                .originalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown")
                .mimeType(mimeType)
                .fileSizeBytes(file.getSize())
                .status(FileStatus.ACTIVE)
                .build());

        // C3: presigned URL TTL = 15 minutes (ADR-FILE-004)
        String presignedUrl = storageService.generatePresignedUrl(storageKey, 15);

        // C5: emit audit event
        auditService.log(AuditAction.FILE_UPLOADED, callerId,
                "UploadedFile", saved.getId().toString(), "uploaded");

        return UploadFileResponse.builder()
                .fileId(saved.getId())
                .originalName(saved.getOriginalName())
                .mimeType(saved.getMimeType())
                .fileSizeBytes(saved.getFileSizeBytes())
                .presignedUrl(presignedUrl)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private String detectMimeType(MultipartFile file) {
        try {
            byte[] header = file.getBytes();
            if (header.length >= 2) {
                if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8) return "image/jpeg";
                if (header.length >= 4
                        && header[0] == 0x25 && header[1] == 0x50
                        && header[2] == 0x44 && header[3] == 0x46) return "application/pdf";
                if (header.length >= 8
                        && header[0] == (byte) 0x89 && header[1] == 0x50
                        && header[2] == 0x4E && header[3] == 0x47) return "image/png";
                if (header.length >= 6
                        && header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46) return "image/gif";
            }
            String declared = file.getContentType();
            return declared != null ? declared : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
