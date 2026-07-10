package com.carebridge.backend.family.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.family.dto.FamilyAlertItemDto;
import com.carebridge.backend.family.dto.FamilyAlertListResponse;
import com.carebridge.backend.family.service.IFamilyAlertService;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FamilyAlertServiceImpl implements IFamilyAlertService {

    private final NotificationRecordRepository notificationRepository;
    private final AuditService auditService;

    @Override
    public FamilyAlertListResponse listFamilyAlerts(UUID callerId, int page, int size) {
        // Step 1: permission-minimized read — only EMERGENCY-typed notifications for this user
        // (ADR-FAM-006: read from notification_records; no cross-domain access to safety_alerts)
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationRecord> records = notificationRepository
                .findByUserIdAndType(callerId, NotificationType.EMERGENCY, pageable);

        List<FamilyAlertItemDto> alerts = records.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        // Step 2: audit safety-relevant alert view (ADR-FAM-008 / POST-3)
        if (!alerts.isEmpty()) {
            auditService.log(AuditAction.FAMILY_ALERT_VIEWED, callerId,
                    "FamilyAlert", callerId.toString(),
                    "Viewed " + alerts.size() + " family alert(s), page=" + page);
        }

        return FamilyAlertListResponse.builder()
                .page(page)
                .size(size)
                .totalItems(records.getTotalElements())
                .alerts(alerts)
                .build();
    }

    private FamilyAlertItemDto toDto(NotificationRecord record) {
        // BR-PRIVACY: only title and body surfaced — no referenceId, no raw user PII
        return FamilyAlertItemDto.builder()
                .alertId(record.getId())
                .title(record.getTitle())
                .body(record.getBody())
                .isRead(record.isRead())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
