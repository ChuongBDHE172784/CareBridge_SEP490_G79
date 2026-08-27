package com.carebridge.backend.family.service.impl;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.SharedDataItemDto;
import com.carebridge.backend.family.dto.SharedDataResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.entity.SharedDataCategory;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.ISharedDataService;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SharedDataServiceImpl implements ISharedDataService {

    private final CareGroupRepository groupRepository;
    private final CareTaskRepository taskRepository;
    private final ReminderRepository reminderRepository;
    private final NotificationRecordRepository notificationRepository;
    private final CareGroupAuthorizationPolicy accessPolicy;

    @Override
    public SharedDataResponse getSharedData(UUID groupId, UUID callerId,
                                             SharedDataCategory category, int page, int size) {
        // Step 1: group must exist
        CareGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));

        // Step 2: caller must be ACCEPTED member (BR-FAM-021)
        if (!accessPolicy.isMember(groupId, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-003",
                    "You are not an accepted member of this care group");
        }

        // Step 3: OWNER sees all; non-OWNER requires the category permission flag (ADR-FAM-003)
        if (!accessPolicy.isOwner(groupId, callerId)) {
            PermissionFlag flag = toPermissionFlag(category);
            if (!accessPolicy.hasPermission(groupId, callerId, flag)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-011",
                        "You do not have permission to view this category");
            }
        }

        // Step 4: query data per category
        List<SharedDataItemDto> items = switch (category) {
            case CALENDAR -> getCalendarItems(groupId, group);
            case LOGS -> getLogItems(groupId);
            case ALERTS -> getAlertItems(groupId, callerId, page, size);
        };

        return SharedDataResponse.builder()
                .groupId(groupId)
                .category(category.name())
                .totalItems(items.size())
                .items(items)
                .asOf(Instant.now())
                .build();
    }

    private PermissionFlag toPermissionFlag(SharedDataCategory category) {
        return switch (category) {
            case CALENDAR -> PermissionFlag.CALENDAR;
            case LOGS -> PermissionFlag.LOGS;
            case ALERTS -> PermissionFlag.ALERTS;
        };
    }

    private List<SharedDataItemDto> getCalendarItems(UUID groupId, CareGroup group) {
        List<SharedDataItemDto> items = taskRepository.findByCareGroupId(groupId).stream()
                .map(this::taskToSharedItem)
                .collect(Collectors.toList());
        if (reminderRepository != null
                && (group.getLinkedJourneyId() != null || group.getLinkedBabyProfileId() != null)) {
            items.addAll(reminderRepository.findSharedAppointments(
                            group.getOwnerUserId(),
                            group.getLinkedJourneyId(),
                            group.getLinkedBabyProfileId()).stream()
                    .map(this::appointmentToSharedItem)
                    .toList());
        }
        return items.stream()
                .sorted(Comparator.comparing(
                        SharedDataItemDto::getOccurredAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private SharedDataItemDto appointmentToSharedItem(Reminder reminder) {
        return SharedDataItemDto.builder()
                .itemId(reminder.getId())
                .itemType("APPOINTMENT")
                .title(reminder.getTitle())
                .summary("APPOINTMENT")
                .occurredAt(reminder.getScheduledAt())
                .status(reminder.getStatus() == null ? null : reminder.getStatus().name())
                .build();
    }

    private SharedDataItemDto taskToSharedItem(CareTask task) {
        return SharedDataItemDto.builder()
                .itemId(task.getId())
                .itemType("TASK")
                .title(task.getTitle())
                .summary(task.getDescription())
                .occurredAt(task.getDueAt())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .build();
    }

    private List<SharedDataItemDto> getLogItems(UUID groupId) {
        // Logs require a cross-domain join through care_groups.linked_journey_id / linked_baby_profile_id.
        // Deferred in v1 — returns empty list (Open Item from UC84 TDS §11.1, OI-4 resolution pending).
        return List.of();
    }

    private List<SharedDataItemDto> getAlertItems(UUID groupId, UUID callerId, int page, int size) {
        List<NotificationRecord> records = notificationRepository
                .findByUserIdAndTypeAndCareGroupId(callerId, NotificationType.EMERGENCY, groupId)
                .stream()
                .sorted(Comparator.comparing(
                        NotificationRecord::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        long requestedOffset = (long) page * size;
        int fromIndex = (int) Math.min(requestedOffset, records.size());
        int toIndex = (int) Math.min(requestedOffset + size, records.size());
        return records.subList(fromIndex, toIndex).stream()
                .map(this::notificationToAlertItem)
                .collect(Collectors.toList());
    }

    private SharedDataItemDto notificationToAlertItem(NotificationRecord record) {
        return SharedDataItemDto.builder()
                .itemId(record.getId())
                .itemType("ALERT")
                .title(record.getTitle())
                .summary(record.getBody())
                .occurredAt(record.getCreatedAt())
                .status(record.isRead() ? "READ" : "UNREAD")
                .build();
    }
}
