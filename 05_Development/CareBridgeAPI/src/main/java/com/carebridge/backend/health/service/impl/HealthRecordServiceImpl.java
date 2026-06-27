package com.carebridge.backend.health.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IStorageService;
import com.carebridge.backend.health.dto.AddHealthRecordRequest;
import com.carebridge.backend.health.dto.AddHealthRecordResponse;
import com.carebridge.backend.health.dto.FileAttachmentDto;
import com.carebridge.backend.health.dto.HealthRecordDetailResponse;
import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordFile;
import com.carebridge.backend.health.entity.HealthRecordStatus;
import com.carebridge.backend.health.repository.HealthRecordFileRepository;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import com.carebridge.backend.health.service.IHealthRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class HealthRecordServiceImpl implements IHealthRecordService {

    private final HealthRecordRepository recordRepository;
    private final HealthRecordFileRepository recordFileRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final IStorageService storageService;
    private final AuditService auditService;

    @Override
    public AddHealthRecordResponse addHealthRecord(AddHealthRecordRequest request, UUID callerId) {
        List<UUID> fileIds = request.getFileIds() != null ? request.getFileIds() : List.of();

        // C1: validate file ownership BEFORE save()
        if (!fileIds.isEmpty()) {
            List<UploadedFile> ownedFiles = uploadedFileRepository
                    .findAllByIdInAndOwnerUserIdAndStatus(fileIds, callerId, FileStatus.ACTIVE);
            if (ownedFiles.size() != fileIds.size()) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "HEALTH-005",
                        "One or more files do not belong to the caller");
            }
        }

        // C4: accountId from JWT
        HealthRecord record = HealthRecord.builder()
                .ownerUserId(callerId)
                .journeyId(request.getJourneyId())
                .babyId(request.getBabyId())
                .recordType(request.getRecordType())
                .title(request.getTitle())
                .recordDate(request.getRecordDate())
                .facilityName(request.getFacilityName())
                .build();

        HealthRecord saved = recordRepository.save(record);

        // Link files to record
        for (int i = 0; i < fileIds.size(); i++) {
            HealthRecordFile link = HealthRecordFile.builder()
                    .healthRecordId(saved.getId())
                    .fileId(fileIds.get(i))
                    .displayOrder(i)
                    .build();
            recordFileRepository.save(link);
        }

        // C3: emit audit event
        auditService.log(AuditAction.HEALTH_RECORD_ADDED, callerId,
                "HealthRecord", saved.getId().toString(), "created");

        return AddHealthRecordResponse.builder()
                .id(saved.getId())
                .recordType(saved.getRecordType().name())
                .title(saved.getTitle())
                .recordDate(saved.getRecordDate())
                .facilityName(saved.getFacilityName())
                .status(saved.getStatus().name())
                .fileIds(fileIds)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public HealthRecordDetailResponse getHealthRecord(UUID recordId, UUID callerId) {
        // C4: ARCHIVED returns 404 (HEALTH-008)
        HealthRecord record = recordRepository.findByIdAndStatus(recordId, HealthRecordStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "HEALTH-008",
                        "Health record not found or archived: " + recordId));

        // C1: ownership check
        if (!record.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HEALTH-004",
                    "Access denied to health record");
        }

        // C2: build attachments with presigned URLs (TTL=15min)
        List<HealthRecordFile> links = recordFileRepository
                .findByHealthRecordIdOrderByDisplayOrderAsc(recordId);

        List<FileAttachmentDto> attachments = links.stream().map(link -> {
            UploadedFile f = uploadedFileRepository.findByIdAndStatus(link.getFileId(), FileStatus.ACTIVE)
                    .orElse(null);
            if (f == null) return null;
            return FileAttachmentDto.builder()
                    .fileId(f.getId())
                    .originalName(f.getOriginalName())
                    .mimeType(f.getMimeType())
                    .displayOrder(link.getDisplayOrder())
                    .presignedUrl(storageService.generatePresignedUrl(f.getStorageKey(), 15))
                    .build();
        }).filter(a -> a != null).collect(Collectors.toList());

        return HealthRecordDetailResponse.builder()
                .id(record.getId())
                .recordType(record.getRecordType().name())
                .title(record.getTitle())
                .recordDate(record.getRecordDate())
                .facilityName(record.getFacilityName())
                .status(record.getStatus().name())
                .attachments(attachments)
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
