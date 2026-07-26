package com.carebridge.backend.family.service.impl;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.CalendarItemDto;
import com.carebridge.backend.family.dto.SharedCareCalendarResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.ICareCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CareCalendarServiceImpl implements ICareCalendarService {

    private final CareGroupRepository groupRepository;
    private final CareTaskRepository taskRepository;
    private final CareGroupAuthorizationPolicy authorizationPolicy;

    @Override
    public SharedCareCalendarResponse getCalendar(UUID groupId, UUID callerId,
                                                   Instant rangeStart, Instant rangeEnd) {
        // Step 1: Care group must exist
        CareGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));

        // Step 2: Caller must be an ACCEPTED member (ADR-FAM-002)
        if (!authorizationPolicy.isMember(groupId, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-003",
                    "You are not an accepted member of this care group");
        }

        // Step 3: Permission check — OWNER sees all; non-OWNER requires calendar=true (ADR-FAM-003)
        if (!authorizationPolicy.isOwner(groupId, callerId)) {
            if (!authorizationPolicy.hasPermission(groupId, callerId, PermissionFlag.CALENDAR)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-007",
                        "Calendar permission not granted");
            }
        }

        // Query canonical manual care tasks only (ADR-FAM-004).
        List<CareTask> tasks = taskRepository
                .findByCareGroupIdAndDueAtBetween(groupId, rangeStart, rangeEnd);

        List<CalendarItemDto> items = tasks.stream()
                .map(this::toCalendarItemDto)
                .collect(Collectors.toList());

        return SharedCareCalendarResponse.builder()
                .groupId(groupId)
                .groupName(group.getGroupName())
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .totalItems(items.size())
                .items(items)
                .build();
    }

    private CalendarItemDto toCalendarItemDto(CareTask task) {
        return CalendarItemDto.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueAt(task.getDueAt())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .assignedTo(task.getAssignedTo())
                .assignedToDisplayName(null) // v1: name resolution not implemented yet
                .build();
    }
}
