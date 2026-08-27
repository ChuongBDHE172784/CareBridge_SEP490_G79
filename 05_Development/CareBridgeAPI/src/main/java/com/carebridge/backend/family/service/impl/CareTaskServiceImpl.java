package com.carebridge.backend.family.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.service.RequiredAuditEvent;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.AssignFamilyTaskRequest;
import com.carebridge.backend.family.dto.AssignFamilyTaskResponse;
import com.carebridge.backend.family.dto.CancelFamilyTaskResponse;
import com.carebridge.backend.family.dto.CareTaskDetailResponse;
import com.carebridge.backend.family.dto.CareTaskDto;
import com.carebridge.backend.family.dto.CareTasksResponse;
import com.carebridge.backend.family.dto.UpdateFamilyTaskRequest;
import com.carebridge.backend.family.dto.UpdateFamilyTaskResponse;
import com.carebridge.backend.family.dto.UpdateTaskStatusRequest;
import com.carebridge.backend.family.dto.UpdateTaskStatusResponse;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.event.CareTaskCancelled;
import com.carebridge.backend.family.event.CareTaskUpdated;
import com.carebridge.backend.family.event.FamilyTaskAssigned;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.ICareTaskService;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CareTaskServiceImpl implements ICareTaskService {

    private final CareGroupRepository groupRepository;
    private final CareGroupMemberRepository memberRepository;
    private final CareTaskRepository taskRepository;
    private final CareGroupAuthorizationPolicy authorizationPolicy;
    private final FcmService fcmService;
    private final DeviceTokenRepository deviceTokenRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    @Override
    public AssignFamilyTaskResponse assignFamilyTask(UUID groupId, AssignFamilyTaskRequest request, UUID callerId) {
        // Step 1: Care group must exist
        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));

        // Step 2: Caller must be ACCEPTED OWNER (ADR-FAM-032)
        if (!authorizationPolicy.canAssignTasks(groupId, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-031",
                    "Only the group owner may assign tasks");
        }

        // Step 3: Assignee must be an ACCEPTED member of the group (ADR-FAM-030)
        CareGroupMember assigneeMember = memberRepository
                .findByCareGroupIdAndUserId(groupId, request.getAssigneeMemberId())
                .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "FAM-030",
                        "Assignee is not a member of this care group"));

        if (assigneeMember.getInviteStatus() != InviteStatus.ACCEPTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-030",
                    "Assignee has not accepted the invitation");
        }

        // Step 4: Due date must be strictly in the future (ADR-FAM-033)
        if (!request.getDueAt().isAfter(Instant.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FAM-032",
                    "Due date must be in the future");
        }

        // Step 5: Persist the task
        CareTask task = CareTask.builder()
                .careGroupId(groupId)
                .assignedBy(callerId)
                .assignedTo(request.getAssigneeMemberId())
                .title(request.getTitle())
                .description(request.getDescription())
                .dueAt(request.getDueAt())
                .origin(ChecklistOrigin.USER_CREATED)
                .targetSubject(request.getTargetSubject())
                .status(CareTaskStatus.OPEN)
                .build();

        CareTask saved = taskRepository.save(task);

        // Step 6: Publish domain event (ADR-FAM-034)
        eventPublisher.publishEvent(new FamilyTaskAssigned(
                UUID.randomUUID(),
                "FamilyTaskAssigned",
                Instant.now(),
                "1.0",
                new FamilyTaskAssigned.Payload(
                        saved.getId(),
                        groupId,
                        callerId,
                        request.getAssigneeMemberId(),
                        request.getTitle(),
                        request.getDueAt()
                ),
                new FamilyTaskAssigned.Metadata(UUID.randomUUID(), "CareTaskServiceImpl")
        ));

        // Step 7: Audit log
        auditService.log(AuditAction.CARE_TASK_ASSIGNED, callerId, "CareTask",
                saved.getId().toString(), "Assigned to " + request.getAssigneeMemberId());

        // Step 8: FCM notification — non-blocking (ADR-FAM-031)
        sendFcmNotification(request.getAssigneeMemberId(), request.getTitle());

        return AssignFamilyTaskResponse.builder()
                .careTaskId(saved.getId())
                .careGroupId(groupId)
                .assignedTo(saved.getAssignedTo())
                .assignedBy(saved.getAssignedBy())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .dueAt(saved.getDueAt())
                .status(saved.getStatus().name())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CareTasksResponse listTasks(UUID groupId, UUID callerId) {
        // Step 1: Care group must exist
        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));

        // Step 2: Caller must be an ACCEPTED member
        if (!memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                groupId, callerId, InviteStatus.ACCEPTED)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-003",
                    "You are not an accepted member of this care group");
        }

        // Step 3: Return all tasks
        List<CareTask> tasks = taskRepository.findByCareGroupId(groupId);
        List<CareTaskDto> dtos = tasks.stream()
                .map(this::toCareTaskDto)
                .collect(Collectors.toList());

        return CareTasksResponse.builder()
                .groupId(groupId)
                .totalTasks(dtos.size())
                .tasks(dtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CareTaskDetailResponse getTaskDetail(UUID groupId, UUID taskId, UUID callerId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));

        if (!memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                groupId, callerId, InviteStatus.ACCEPTED)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-068",
                    "You are not an accepted member of this care group");
        }

        CareTask task = taskRepository.findByIdAndCareGroupId(taskId, groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-033",
                        "Care task not found"));

        return toDetailResponse(task);
    }

    @Override
    public UpdateFamilyTaskResponse updateFamilyTask(UUID groupId, UUID taskId,
                                                     UpdateFamilyTaskRequest request, UUID callerId) {
        if (!authorizationPolicy.canUpdateTask(groupId, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-072",
                    "Only the care group owner can update tasks");
        }

        CareTask task = taskRepository.findByIdAndCareGroupId(taskId, groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-033",
                        "Care task not found"));

        if (!isIncomplete(task.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-073",
                    "Only incomplete tasks (OPEN or IN_PROGRESS) can be updated");
        }

        if (request == null || (request.getTitle() == null
                && request.getDescription() == null
                && request.getDueAt() == null
                && request.getAssigneeMemberId() == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FAM-076",
                    "No updatable fields provided");
        }

        UUID previousAssignee = task.getAssignedTo();
        List<String> changedFields = new ArrayList<>();

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank() || request.getTitle().length() > 255) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "FAM-075",
                        "Validation failed");
            }
            if (!Objects.equals(task.getTitle(), request.getTitle())) {
                changedFields.add("title");
            }
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            if (!Objects.equals(task.getDescription(), request.getDescription())) {
                changedFields.add("description");
            }
            task.setDescription(request.getDescription());
        }
        if (request.getDueAt() != null) {
            if (!request.getDueAt().isAfter(Instant.now())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "FAM-075",
                        "Validation failed");
            }
            if (!Objects.equals(task.getDueAt(), request.getDueAt())) {
                changedFields.add("dueAt");
            }
            task.setDueAt(request.getDueAt());
        }
        if (request.getAssigneeMemberId() != null) {
            CareGroupMember assignee = memberRepository.findByIdAndCareGroupId(
                            request.getAssigneeMemberId(), groupId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "FAM-074",
                            "New assignee is not an accepted member of this care group"));
            if (assignee.getInviteStatus() != InviteStatus.ACCEPTED) {
                throw new BusinessException(HttpStatus.CONFLICT, "FAM-074",
                        "New assignee is not an accepted member of this care group");
            }
            if (!Objects.equals(task.getAssignedTo(), assignee.getUserId())) {
                changedFields.add("assignedTo");
            }
            task.setAssignedTo(assignee.getUserId());
        }

        CareTask saved = taskRepository.save(task);
        auditService.log(AuditAction.CARE_TASK_UPDATED, callerId,
                "CareTask", saved.getId().toString(), String.join(",", changedFields));
        eventPublisher.publishEvent(new CareTaskUpdated(
                UUID.randomUUID(), "CareTaskUpdated", Instant.now(), "1.0",
                new CareTaskUpdated.Payload(saved.getId(), groupId, previousAssignee,
                        saved.getAssignedTo(), changedFields),
                new CareTaskUpdated.Metadata(UUID.randomUUID(), "CareTaskServiceImpl")));

        return toUpdateResponse(saved);
    }

    @Override
    public CancelFamilyTaskResponse cancelFamilyTask(UUID groupId, UUID taskId, UUID callerId) {
        if (!authorizationPolicy.canCancelTask(groupId, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-079",
                    "Only the care group owner can cancel tasks");
        }

        CareTask task = taskRepository.findByIdAndCareGroupId(taskId, groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-033",
                        "Care task not found"));

        if (task.getStatus() == CareTaskStatus.DONE) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-080",
                    "A completed task cannot be cancelled");
        }
        if (task.getStatus() == CareTaskStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-081",
                    "This task is already cancelled");
        }
        if (task.getStatus() != CareTaskStatus.OPEN && task.getStatus() != CareTaskStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-080",
                    "Only incomplete tasks can be cancelled");
        }

        task.setStatus(CareTaskStatus.CANCELLED);
        CareTask saved = taskRepository.save(task);
        auditService.log(AuditAction.CARE_TASK_CANCELLED, callerId,
                "CareTask", saved.getId().toString(), "task cancelled");
        eventPublisher.publishEvent(new CareTaskCancelled(
                UUID.randomUUID(), "CareTaskCancelled", Instant.now(), "1.0",
                new CareTaskCancelled.Payload(saved.getId(), groupId, saved.getAssignedTo(),
                        saved.getAssignedBy(), callerId, saved.getTitle()),
                new CareTaskCancelled.Metadata(UUID.randomUUID(), "CareTaskServiceImpl")));

        return toCancelResponse(saved);
    }

    @Override
    @Transactional
    public UpdateTaskStatusResponse updateTaskStatus(UUID groupId, UUID taskId,
                                                     UpdateTaskStatusRequest request, UUID callerId) {
        // Step 1: care group must exist
        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));

        // Step 2: task must exist within the group
        CareTask task = taskRepository.findByIdAndCareGroupId(taskId, groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-033",
                        "Task not found in this care group"));

        // Step 3: only the assignee may update status (ADR-FAM-006)
        if (!callerId.equals(task.getAssignedTo())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-034",
                    "Only the task assignee may update the status");
        }

        // Step 4: parse and validate the requested status
        CareTaskStatus requested;
        try {
            requested = CareTaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FAM-035",
                    "Invalid status value: " + request.getStatus());
        }

        // Step 5: FSM transition check (ADR-FAM-005)
        CareTaskStatus current = task.getStatus();
        if (!current.canTransitionTo(requested)) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-023",
                    "Cannot transition from " + current + " to " + requested);
        }

        // Step 6: apply transition
        task.setStatus(requested);
        Instant now = Instant.now();
        if (requested == CareTaskStatus.DONE) {
            task.setCompletedAt(now);
        }
        CareTask saved = taskRepository.save(task);

        // Step 7: required typed audit log
        auditCareTaskStatus(task, callerId, current, requested);

        // Step 8: notify assigner — best-effort
        if (task.getAssignedBy() != null) {
            try {
                List<com.carebridge.backend.notification.entity.DeviceToken> tokens =
                        deviceTokenRepository.findByUserIdAndActiveTrue(task.getAssignedBy());
                for (var token : tokens) {
                    fcmService.sendToToken(token.getToken(), "Task status updated",
                            "Task \"" + task.getTitle() + "\" is now " + requested.name());
                }
            } catch (Exception e) {
                log.warn("FCM notification failed after task status update (non-blocking): {}", e.getMessage());
            }
        }

        return UpdateTaskStatusResponse.builder()
                .careTaskId(saved.getId())
                .careGroupId(groupId)
                .status(saved.getStatus().name())
                .completedAt(saved.getCompletedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    private CareTaskDto toCareTaskDto(CareTask task) {
        return CareTaskDto.builder()
                .careTaskId(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueAt(task.getDueAt())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .assignedTo(task.getAssignedTo())
                .assignedBy(task.getAssignedBy())
                .completedAt(task.getCompletedAt())
                .build();
    }

    private void auditCareTaskStatus(
            CareTask task,
            UUID callerId,
            CareTaskStatus previousStatus,
            CareTaskStatus nextStatus) {
        ChecklistCareContextType contextType = task.getBabyId() != null
                ? ChecklistCareContextType.BABY
                : task.getJourneyId() != null ? ChecklistCareContextType.JOURNEY : null;
        UUID contextId = task.getBabyId() != null ? task.getBabyId() : task.getJourneyId();
        if (contextId == null) {
            var group = groupRepository.findById(task.getCareGroupId()).orElse(null);
            if (group != null && group.getLinkedBabyProfileId() != null) {
                contextType = ChecklistCareContextType.BABY;
                contextId = group.getLinkedBabyProfileId();
            } else if (group != null && group.getLinkedJourneyId() != null) {
                contextType = ChecklistCareContextType.JOURNEY;
                contextId = group.getLinkedJourneyId();
            }
        }
        auditService.logRequired(new RequiredAuditEvent(
                AuditAction.CARE_TASK_STATUS_UPDATED,
                callerId,
                "USER",
                null,
                task.getAssignedTo(),
                "CARE_TASK",
                task.getId(),
                contextType,
                contextId,
                null,
                null,
                Map.of("status", previousStatus.name()),
                Map.of("status", nextStatus.name()),
                "USER_ACTION",
                UUID.randomUUID()));
    }

    private CareTaskDetailResponse toDetailResponse(CareTask task) {
        return CareTaskDetailResponse.builder()
                .careTaskId(task.getId())
                .careGroupId(task.getCareGroupId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueAt(task.getDueAt())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .assignedTo(task.getAssignedTo())
                .assignedToName(resolveName(task.getAssignedTo()))
                .assignedBy(task.getAssignedBy())
                .assignedByName(resolveName(task.getAssignedBy()))
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private UpdateFamilyTaskResponse toUpdateResponse(CareTask task) {
        return UpdateFamilyTaskResponse.builder()
                .careTaskId(task.getId())
                .careGroupId(task.getCareGroupId())
                .assignedTo(task.getAssignedTo())
                .assignedBy(task.getAssignedBy())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueAt(task.getDueAt())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .completedAt(task.getCompletedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private CancelFamilyTaskResponse toCancelResponse(CareTask task) {
        return CancelFamilyTaskResponse.builder()
                .careTaskId(task.getId())
                .careGroupId(task.getCareGroupId())
                .assignedTo(task.getAssignedTo())
                .assignedBy(task.getAssignedBy())
                .title(task.getTitle())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private boolean isIncomplete(CareTaskStatus status) {
        return status == CareTaskStatus.OPEN || status == CareTaskStatus.IN_PROGRESS;
    }

    private String resolveName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(user -> user.getName())
                .orElse(null);
    }

    private void sendFcmNotification(UUID assigneeId, String taskTitle) {
        try {
            List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(assigneeId);
            for (DeviceToken token : tokens) {
                fcmService.sendToToken(token.getToken(), "New Task Assigned",
                        "You have been assigned: " + taskTitle);
            }
        } catch (Exception e) {
            log.warn("FCM notification failed for assignee {}: {}", assigneeId, e.getMessage());
        }
    }
}
