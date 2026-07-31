package com.carebridge.backend.checklist.today.service;

import com.carebridge.backend.checklist.repository.ChecklistActionCommandRepository;
import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.provider.AuthorizedTask;
import com.carebridge.backend.checklist.today.provider.TaskActionHandler;
import com.carebridge.backend.checklist.entity.ChecklistActionCommand;
import com.carebridge.backend.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UnifiedTaskActionFacade {
    private static final Set<String> CONTROLLED_SKIP_REASONS = Set.of(
            "NOT_APPLICABLE", "USER_CHOICE", "LIFECYCLE_CHANGED");
    private final Map<TaskKind, TaskActionHandler> handlers;
    private final ChecklistActionCommandRepository commandRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public UnifiedTaskActionFacade(List<TaskActionHandler> handlers,
                                   ChecklistActionCommandRepository commandRepository,
                                   ObjectMapper objectMapper) {
        this(handlers, commandRepository, objectMapper, Clock.systemUTC());
    }

    public UnifiedTaskActionFacade(List<TaskActionHandler> handlers,
                                   ChecklistActionCommandRepository commandRepository,
                                   ObjectMapper objectMapper,
                                   Clock clock) {
        this.handlers = handlers.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                TaskActionHandler::taskKind, handler -> handler));
        this.commandRepository = commandRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public TaskActionResponse apply(UUID actorUserId, TaskKind taskKind, UUID taskId,
                                    TaskActionRequest request) {
        TaskActionHandler handler = handlers.get(taskKind);
        if (handler == null) {
            throw taskNotFound();
        }

        // Authorization is deliberately first. A command replay must never reveal another
        // recipient's task or turn a revoked membership into a successful response.
        AuthorizedTask authorized;
        try {
            authorized = handler.authorize(actorUserId, taskId);
        } catch (BusinessException exception) {
            if (taskKind != TaskKind.REMINDER
                    || !"TASK_NOT_FOUND".equals(exception.getCode())
                    || request == null
                    || request.clientRequestId() == null) {
                throw exception;
            }
            var historicalCommand = commandRepository
                    .findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                            actorUserId, taskKind.name(), taskId, request.clientRequestId())
                    .filter(command -> command.getReminderDefinitionId() != null)
                    .orElseThrow(() -> exception);
            authorized = handler.authorizeReplay(
                    actorUserId, taskId, historicalCommand.getReminderDefinitionId());
        }
        UUID canonicalTaskId = authorized.taskId();
        if (request == null || request.action() == null || request.clientRequestId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TASK_ACTION_INVALID",
                    "Action and clientRequestId are required");
        }
        if (request.action() == TaskAction.SKIP
                && (request.reason() == null || request.reason().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TASK_ACTION_REASON_REQUIRED",
                    "Skip requires a controlled reason");
        }
        if (request.action() == TaskAction.SKIP
                && !CONTROLLED_SKIP_REASONS.contains(request.reason())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TASK_ACTION_REASON_INVALID",
                    "Skip reason is not controlled");
        }
        if (request.action() == TaskAction.COMPLETE && request.reason() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TASK_ACTION_REASON_INVALID",
                    "Complete does not accept a reason");
        }

        String payloadHash = hash(taskKind, canonicalTaskId, request);
        commandRepository.acquireTaskActionLock(
                taskActionScope(taskKind, handler.actionScopeId(authorized)));
        commandRepository.acquireIdempotencyClaimLock(
                idempotencyScope(actorUserId, taskKind, canonicalTaskId, request.clientRequestId()));
        var existing = commandRepository.findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
                actorUserId, taskKind.name(), canonicalTaskId, request.clientRequestId());
        if (existing.isPresent()) {
            var command = existing.get();
            if (!payloadHash.equals(command.getPayloadHash())) {
                throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSE",
                        "clientRequestId was already used with a different payload");
            }
            try {
                return objectMapper.readValue(command.getResultJson(), TaskActionResponse.class).asReplay();
            } catch (Exception exception) {
                throw new IllegalStateException("Stored task action result is unreadable", exception);
            }
        }
        var current = handler.authorizeForUpdate(actorUserId, authorized);
        if (!current.allowedActions().contains(request.action())) {
            if (isTerminal(current.status())) {
                throw new BusinessException(HttpStatus.CONFLICT, "TASK_ALREADY_TERMINAL",
                        "Task is already terminal");
            }
            throw new BusinessException(HttpStatus.CONFLICT, "ACTION_NOT_ALLOWED",
                    "Requested action is not currently allowed");
        }

        Instant appliedAt = clock.instant();
        UUID correlationId = UUID.randomUUID();
        TaskActionResponse response = handler.apply(current, actorUserId, request.action(),
                request.reason(), appliedAt, correlationId);
        try {
            ChecklistActionCommand command = ChecklistActionCommand.builder()
                    .actorUserId(actorUserId)
                    .taskKind(taskKind.name())
                    .taskId(canonicalTaskId)
                    .reminderDefinitionId(taskKind == TaskKind.REMINDER ? current.instanceId() : null)
                    .clientRequestId(request.clientRequestId())
                    .payloadHash(payloadHash)
                    .actionType(request.action().name())
                    .resultStatus("APPLIED")
                    .resultJson(objectMapper.writeValueAsString(response))
                    .appliedAt(appliedAt)
                    .retainUntil(ZonedDateTime.ofInstant(appliedAt, ZoneOffset.UTC)
                            .plusYears(7).toInstant())
                    .build();
            commandRepository.save(command);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist task action command", exception);
        }
        return response;
    }

    private static String hash(TaskKind taskKind, UUID taskId, TaskActionRequest request) {
        String canonical = taskKind.name() + "|" + taskId + "|" + request.action().name()
                + "|" + (request.reason() == null ? "" : request.reason());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String idempotencyScope(
            UUID actorUserId,
            TaskKind taskKind,
            UUID taskId,
            UUID clientRequestId) {
        return actorUserId + "|" + taskKind.name() + "|" + taskId + "|" + clientRequestId;
    }

    private static String taskActionScope(TaskKind taskKind, UUID taskId) {
        return "TASK_ACTION|" + taskKind.name() + "|" + taskId;
    }

    private static boolean isTerminal(String status) {
        return "COMPLETED".equals(status)
                || "DONE".equals(status)
                || "SKIPPED".equals(status)
                || "CANCELLED".equals(status);
    }

    private static BusinessException taskNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found");
    }
}
