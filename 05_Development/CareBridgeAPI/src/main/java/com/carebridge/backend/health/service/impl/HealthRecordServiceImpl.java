package com.carebridge.backend.health.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IStorageService;
import com.carebridge.backend.health.dto.*;
import com.carebridge.backend.health.dto.AddHealthRecordRequest;
import com.carebridge.backend.health.dto.AddHealthRecordResponse;
import com.carebridge.backend.health.dto.FileAttachmentDto;
import com.carebridge.backend.health.dto.HealthRecordDetailResponse;
import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordFile;
import com.carebridge.backend.health.entity.HealthRecordStatus;
import com.carebridge.backend.health.entity.RecordType;
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
                .sourceName(request.getFacilityName())
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
                .facilityName(saved.getSourceName())
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
                .facilityName(record.getSourceName())
                .status(record.getStatus().name())
                .attachments(attachments)
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    @Override
    public UpdateHealthRecordResponse updateHealthRecord(UUID id, UpdateHealthRecordRequest request, UUID ownerUserId) {
        // C1: find record, throw 404 if not found
        HealthRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "HEALTH-007",
                        "Health record not found: " + id));

        // C2: ownership check
        if (!record.getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HEALTH-004",
                    "Access denied to health record");
        }

        // C1: status check — only ACTIVE records can be updated
        if (record.getStatus() == HealthRecordStatus.ARCHIVED) {
            throw new BusinessException(HttpStatus.CONFLICT, "HEALTH-006",
                    "Cannot update archived health record");
        }

        // C3: PATCH — apply only non-null fields
        if (request.getTitle() != null) record.setTitle(request.getTitle());
        if (request.getRecordType() != null) record.setRecordType(RecordType.valueOf(request.getRecordType()));
        if (request.getRecordDate() != null) record.setRecordDate(request.getRecordDate());
        if (request.getSourceType() != null) record.setSourceType(request.getSourceType());
        if (request.getSourceName() != null) record.setSourceName(request.getSourceName());
        if (request.getFileUrl() != null) record.setFileUrl(request.getFileUrl());
        if (request.getBabyId() != null) record.setBabyId(request.getBabyId());
        if (request.getJourneyId() != null) record.setJourneyId(request.getJourneyId());

        HealthRecord saved = recordRepository.save(record);

        // C5: emit audit event
        auditService.log(AuditAction.HEALTH_RECORD_UPDATED, ownerUserId,
                "HealthRecord", saved.getId().toString(), "updated");

        return UpdateHealthRecordResponse.builder()
                .healthRecordId(saved.getId())
                .title(saved.getTitle())
                .recordType(saved.getRecordType().name())
                .recordDate(saved.getRecordDate())
                .sourceType(saved.getSourceType())
                .sourceName(saved.getSourceName())
                .fileUrl(saved.getFileUrl())
                .status(saved.getStatus().name())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    public ArchiveHealthRecordResponse archiveRecord(UUID id, UUID ownerUserId) {
        // C1: find record, throw 404 if not found
        HealthRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "HEALTH-007",
                        "Health record not found: " + id));

        // C2: ownership check
        if (!record.getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HEALTH-004",
                    "Access denied to health record");
        }

        // C3: idempotent — already ARCHIVED, return early without save or audit
        if (record.getStatus() == HealthRecordStatus.ARCHIVED) {
            return ArchiveHealthRecordResponse.builder()
                    .healthRecordId(record.getId())
                    .status(record.getStatus().name())
                    .updatedAt(record.getUpdatedAt())
                    .build();
        }

        // C1: soft-delete — set status=ARCHIVED, never physical DELETE
        record.setStatus(HealthRecordStatus.ARCHIVED);
        HealthRecord saved = recordRepository.save(record);

        // C5: emit audit event only on actual ACTIVE→ARCHIVED transition
        auditService.log(AuditAction.HEALTH_RECORD_ARCHIVED, ownerUserId,
                "HealthRecord", saved.getId().toString(), "archived");

        return ArchiveHealthRecordResponse.builder()
                .healthRecordId(saved.getId())
                .status(saved.getStatus().name())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TimelineResponse getTimeline(UUID ownerUserId, TimelineFilter filter) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(filter.getPage(), filter.getSize());

        org.springframework.data.domain.Page<HealthRecord> page =
                recordRepository.findActiveByOwnerFiltered(
                        ownerUserId,
                        filter.getRecordType(),
                        filter.getJourneyId(),
                        filter.getBabyId(),
                        filter.getSourceType(),
                        pageable);

        java.util.List<HealthRecordTimelineItem> items = page.getContent().stream()
                .map(r -> HealthRecordTimelineItem.builder()
                        .healthRecordId(r.getId())
                        .recordType(r.getRecordType().name())
                        .title(r.getTitle())
                        .recordDate(r.getRecordDate())
                        .sourceType(r.getSourceType())
                        .sourceName(r.getSourceName())
                        .fileUrl(r.getFileUrl())
                        .journeyId(r.getJourneyId())
                        .babyId(r.getBabyId())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return TimelineResponse.builder()
                .items(items)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }
}
