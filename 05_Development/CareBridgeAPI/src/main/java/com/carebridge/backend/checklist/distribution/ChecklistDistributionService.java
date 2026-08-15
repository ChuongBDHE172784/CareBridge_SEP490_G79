package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.checklist.audit.ChecklistAuditActorType;
import com.carebridge.backend.checklist.audit.ChecklistAuditEvent;
import com.carebridge.backend.checklist.audit.ChecklistAuditResourceType;
import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistMaterializationMode;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.checklist.repository.ChecklistActionCommandRepository;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional create-or-reuse boundary shared by event and scheduled reconciliation paths. */
@Service
public class ChecklistDistributionService {

    private static final String DISTRIBUTOR = "CHECKLIST_DISTRIBUTOR";
    private static final String OBSOLETE = "LIFECYCLE_WINDOW_OBSOLETE";
    private static final String CADENCE_PERIOD_CLOSED = "CADENCE_PERIOD_CLOSED";
    private static final String OWNER_MISMATCH = "CONTEXT_OWNER_MISMATCH";
    private static final String KEY_CONFLICT = "DISTRIBUTION_KEY_CONFLICT";
    static final String ITEM_DUE_ANCHOR_MISSING = "ITEM_DUE_ANCHOR_MISSING";
    private final ChecklistInstanceRepository instanceRepository;
    private final ChecklistTaskInstanceRepository taskRepository;
    private final ChecklistActionCommandRepository commandRepository;
    private final ChecklistAuditWriter auditWriter;
    private final ChecklistLifecycleEligibilityService eligibilityService;
    private final Clock clock;
    private final ChecklistFamilyPermissionPolicy familyPermissionPolicy = new ChecklistFamilyPermissionPolicy();

    @Autowired
    public ChecklistDistributionService(
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            ChecklistActionCommandRepository commandRepository,
            ChecklistAuditWriter auditWriter) {
        this(instanceRepository, taskRepository, commandRepository, auditWriter,
                new ChecklistLifecycleEligibilityService(), Clock.systemUTC());
    }

    public ChecklistDistributionService(
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            ChecklistActionCommandRepository commandRepository,
            ChecklistAuditWriter auditWriter,
            ChecklistLifecycleEligibilityService eligibilityService,
            Clock clock) {
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.commandRepository = commandRepository;
        this.auditWriter = auditWriter;
        this.eligibilityService = eligibilityService;
        this.clock = clock;
    }

    // =========================================================================
    // HÀM SERVICE CHÍNH THỰC THI LUỒNG PHÂN PHỐI VIỆC CẦN LÀM (CHECKLIST DISTRIBUTION)
    // =========================================================================
    @Transactional
    public ChecklistDistributionResult distribute(ChecklistDistributionCommand command) {
        return distributeDetailed(command).total();
    }

    /**
     * Phân phối việc cần làm chi tiết theo từng người nhận (Mẹ / Người thân trong Care Group).
     * Đảm bảo tính Idempotent (chạy nhiều lần không trùng lặp) và kiểm tra tính hợp lệ vòng đời y tế.
     *
     * @param command Lệnh phân phối chứa thông tin template, chu kỳ, người nhận, ngữ cảnh hành trình
     * @return ChecklistDistributionExecutionResult Kết quả chi tiết số lượng instance/task tạo mới, tồn tại, hủy hoặc xung đột
     */
    @Transactional
    public ChecklistDistributionExecutionResult distributeDetailed(ChecklistDistributionCommand command) {
        // [BƯỚC 1 & 2: Validate nghiệp vụ & Tiếp nhận Request]
        // 1. Kiểm tra cấu trúc Command: bắt buộc phải có version template, context, timezone, người nhận, danh sách task items
        validateCommand(command);

        // 2. Validate quyền sở hữu ngữ cảnh (Context Owner Check): Chủ nhóm chăm sóc phải trùng khớp với chủ context (Mẹ)
        if (!command.careGroupOwnerUserId().equals(command.contextOwnerUserId())) {
            recordFailure(command, OWNER_MISMATCH, command.contextId());
            ChecklistDistributionResult result = new ChecklistDistributionResult(0, 0, 0, 0, 0, 0, 1, 0);
            return commandResult(result, OWNER_MISMATCH);
        }

        // 3. Kiểm tra tính hợp lệ về mặt Vòng đời y tế (Giai đoạn thai kỳ / Sau sinh / Tuần tuổi bé)
        ChecklistEligibilityDecision decision = eligibilityService.evaluate(
                command.stage(), command.substage(), command.lifecycleDates(), command.effectiveDate());
        
        // 3.1. Nếu gặp lỗi dữ liệu mốc thời gian (thiếu LMP/EDD/BirthDate), ghi nhận thất bại và dừng
        if (decision.failureCode() != null) {
            auditFailure(command, decision.failureCode());
            ChecklistDistributionResult result = new ChecklistDistributionResult(0, 0, 0, 0, 0, 0, 0, 1);
            return commandResult(result, decision.failureCode());
        }

        // 3.2. Nếu người dùng chưa đến hoặc đã qua giai đoạn áp dụng checklist này (chưa đủ tuần hoặc quá tuần), bỏ qua
        if (!decision.eligible()) {
            List<ChecklistRecipientDistributionResult> recipients = command.recipients().stream()
                    .filter(Objects::nonNull)
                    .map(recipient -> new ChecklistRecipientDistributionResult(
                            recipient.userId(), decision.windowStart(), decision.windowEnd(),
                            new ChecklistDistributionResult(0, 0, 0, 0, 0, 0, 0, 0),
                            "LIFECYCLE_NOT_ELIGIBLE"))
                    .toList();
            return new ChecklistDistributionExecutionResult(
                    new ChecklistDistributionResult(0, 0, 0, 0, 0, 0, 0, 0), recipients);
        }

        // 3.3. Kiểm tra mốc neo tính ngày hết hạn (Anchor Due Date) của từng task con
        boolean missingItemAnchor = command.items().stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> item.dueAnchor() != null
                        && item.dueAnchor() != com.carebridge.backend.checklist.model.ChecklistAnchorType.NONE
                        && item.dueOffsetDays() != null
                        && !eligibilityService.hasAnchor(item.dueAnchor(), command.lifecycleDates()));
        if (missingItemAnchor) {
            auditFailure(command, ITEM_DUE_ANCHOR_MISSING);
            ChecklistDistributionResult result = new ChecklistDistributionResult(0, 0, 0, 0, 0, 0, 0, 1);
            return commandResult(result, ITEM_DUE_ANCHOR_MISSING);
        }

        // [BƯỚC 3.1: Khóa bi quan chống Race-Condition & Thu thập danh sách phân phối]
        Counters totals = new Counters();
        List<ChecklistRecipientDistributionResult> recipientResults = new ArrayList<>();

        // Khóa theo Distribution Key để tránh trường hợp nhiều luồng cùng phân phối 1 checklist cho 1 user cùng lúc
        command.recipients().stream()
                .filter(recipient -> mayReceive(command, recipient))
                .map(recipient -> lifecycleScopeKey(command, recipient))
                .distinct()
                .sorted()
                .forEach(instanceRepository::acquireDistributionKeyLock);

        // [BƯỚC 3.2: Lặp qua từng Người nhận (Mẹ / Thành viên Care Group) để phân phối việc]
        for (ChecklistDistributionRecipient recipient : command.recipients()) {
            Counters counters = new Counters();

            // Kiểm tra quyền nhận checklist của người nhận (Role MOTHER hoặc thành viên Family có quyền VIEW)
            if (!mayReceive(command, recipient)) {
                counters.denied++;
                totals.add(counters);
                recipientResults.add(new ChecklistRecipientDistributionResult(
                        recipient == null ? null : recipient.userId(), decision.windowStart(), decision.windowEnd(),
                        counters.result(), "RECIPIENT_NOT_ELIGIBLE"));
                continue;
            }

            // Quét và hủy/chuyển vào lịch sử các checklist cũ đã hết hạn nếu đang ở chu kỳ mới (trừ chế độ catch-up bù lịch sử)
            if (!isCatchUp(command)) {
                cancelObsoletePending(command, recipient, decision, counters);
            }

            // Thực thi phân phối checklist và các task con cho người nhận cụ thể này
            distributeToRecipient(command, recipient, decision, counters);
            totals.add(counters);
            recipientResults.add(new ChecklistRecipientDistributionResult(
                    recipient.userId(), decision.windowStart(), decision.windowEnd(), counters.result(), null));
        }

        // [BƯỚC 4 & 5: Đóng gói phản hồi & Trạng thái Database]
        if (recipientResults.isEmpty()) {
            return commandResult(new ChecklistDistributionResult(0, 0, 0, 0, 0, 0, 0, 1),
                    "RECIPIENT_NOT_RESOLVED");
        }
        return new ChecklistDistributionExecutionResult(totals.result(), recipientResults);
    }

    private static ChecklistDistributionExecutionResult commandResult(
            ChecklistDistributionResult result,
            String dispositionCode) {
        return new ChecklistDistributionExecutionResult(result,
                List.of(new ChecklistRecipientDistributionResult(null, null, null, result, dispositionCode)));
    }

    /**
     * Phân phối checklist và các task con cho một người nhận cụ thể (Mẹ hoặc Người thân).
     * Tạo ChecklistInstance và ChecklistTaskInstance tương ứng, đảm bảo tính duy nhất qua Distribution Key.
     */
    private void distributeToRecipient(
            ChecklistDistributionCommand command,
            ChecklistDistributionRecipient recipient,
            ChecklistEligibilityDecision decision,
            Counters counters) {
        // Tạo token mốc cửa sổ thời gian (windowStart, windowEnd)
        String windowStartToken = decision.windowStart() == null ? "NONE" : decision.windowStart().toString();
        String windowEndToken = decision.windowEnd() == null ? "NONE" : decision.windowEnd().toString();
        UUID recipientCareGroupId = recipientCareGroupId(command, recipient);

        // Sinh Distribution Key chuẩn hóa để đảm bảo Idempotency (chống trùng lặp)
        String key = command.cadence() == null
                ? Short.valueOf((short) 2).equals(materializedContractVersion(command))
                        ? ChecklistDistributionKeyFactory.instanceKey(
                        command.templateVersionId(), recipient.userId(), recipient.role().name(),
                        recipientCareGroupId, command.contextType().name(), command.contextId(),
                        windowStartToken, windowEndToken, command.gestationalDatingRevision(), (short) 2)
                        : ChecklistDistributionKeyFactory.instanceKey(
                        command.templateVersionId(), recipient.userId(), recipient.role().name(),
                        recipientCareGroupId, command.contextType().name(), command.contextId(),
                        windowStartToken, windowEndToken, command.gestationalDatingRevision())
                : ChecklistDistributionKeyFactory.cadenceInstanceKey(
                        command.templateVersionId(), recipient.userId(), recipient.role().name(),
                        recipientCareGroupId, command.contextType().name(), command.contextId(),
                        command.cadence().scheduleType().name(),
                        command.cadence().materializationPolicy().name(),
                        command.cadence().periodKey(),
                        command.cadence().scheduleZone().getId(),
                        command.gestationalDatingRevision());

        // Kiểm tra xem ChecklistInstance này đã tồn tại trong DB chưa
        ChecklistInstance instance = instanceRepository.findByDistributionKey(key).orElse(null);

        // Fallback tìm kiếm theo định danh logic cũ đối với Mẹ (nếu là V1 template)
        if (instance == null && isPersonalMother(recipient)
                && !Short.valueOf((short) 2).equals(materializedContractVersion(command))) {
            List<ChecklistInstance> legacy = instanceRepository.findAllByLogicalPersonalIdentity(
                            recipient.userId(), recipient.role(), command.contextType(), command.contextId(),
                            command.templateVersionId(), ChecklistOrigin.SYSTEM_TEMPLATE)
                    .stream()
                    .filter(candidate -> candidate.getStatus() != ChecklistInstanceStatus.CANCELLED)
                    .filter(candidate -> candidate.getHistoricalAt() == null)
                    .filter(candidate -> Objects.equals(
                            candidate.getGestationalDatingRevision(), command.gestationalDatingRevision()))
                    .filter(candidate -> sameWindow(candidate, decision))
                    .toList();
            if (!legacy.isEmpty()) {
                instance = canonicalPersonalInstance(legacy);
                instance = instanceRepository.findForUpdateById(instance.getId()).orElse(null);
            }
        }

        // Xử lý xung đột dữ liệu nếu instance đã tồn tại nhưng không khớp metadata
        if (instance != null && !matches(instance, command, recipient, decision)) {
            if (sameOccurrenceIdentity(instance, command, recipient, decision)) {
                if (isCatchUp(command)) {
                    counters.existingInstances++;
                    closeCatchUpOccurrence(instance, command, recipient, counters);
                    return;
                }
                if (isCatchUpInstance(instance)) {
                    counters.existingInstances++;
                    return;
                }
            }
            recordFailure(command, KEY_CONFLICT, instance.getId());
            counters.conflicts++;
            return;
        }

        // Nếu chưa tồn tại -> Tạo mới ChecklistInstance (Đợt việc cần làm cha)
        if (instance == null) {
            ChecklistInstance proposed = ChecklistInstance.builder()
                    .distributionKey(key)
                    .templateLineageId(command.templateLineageId())
                    .templateVersionId(command.templateVersionId())
                     .recipientUserId(recipient.userId())
                     .recipientRole(recipient.role())
                     .careGroupId(recipientCareGroupId)
                     .careGroupMemberId(recipient.role() == ChecklistRecipientRole.FAMILY
                             ? recipient.careGroupMemberId() : null)
                     .checklistAccessEpoch(recipient.role() == ChecklistRecipientRole.FAMILY
                             ? recipient.checklistAccessEpoch() : null)
                     .careContextType(command.contextType())
                    .careContextId(command.contextId())
                    .contextOwnerUserId(command.contextOwnerUserId())
                    .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                    .gestationalDatingRevision(command.gestationalDatingRevision())
                    .keyVersion(materializedKeyVersion(command))
                    .periodKey(materializedPeriodKey(command))
                    .scheduleZoneId(materializedScheduleZoneId(command))
                    .checklistContractVersion(materializedContractVersion(command))
                    .materializationMode(materializedMode(command))
                    .wasActionable(materializedWasActionable(command))
                    .windowStart(decision.windowStart())
                    .windowEnd(decision.windowEnd())
                    .status(ChecklistInstanceStatus.PENDING)
                    .build();
            try {
                // Lưu ChecklistInstance vào cơ sở dữ liệu
                instance = instanceRepository.save(proposed);
                counters.createdInstances++;

                // Ghi Audit Log hành động phân phối checklist instance
                auditWriter.write(event(AuditAction.CHECKLIST_DISTRIBUTED, command, recipient.userId(),
                        ChecklistAuditResourceType.CHECKLIST_INSTANCE, instance.getId(),
                        null, null, ChecklistInstanceStatus.PENDING.name(), null));
            } catch (DataIntegrityViolationException loser) {
                // Xử lý concurrency winner-loser: lấy instance đã được luồng khác tạo trước đó
                instance = instanceRepository.findByDistributionKey(key).orElseThrow(() -> loser);
                if (!matches(instance, command, recipient, decision)) {
                    recordFailure(command, KEY_CONFLICT, instance.getId());
                    counters.conflicts++;
                    return;
                }
                counters.existingInstances++;
            }
        } else {
            counters.existingInstances++;
        }

        // [Tạo các ChecklistTaskInstance con tương ứng từ template]
        ChecklistInstance lockedParent = instance;
        List<ChecklistTaskInstance> lockedTasks = taskRepository
                .findAllForUpdateByChecklistInstanceIdOrderByTaskKey(lockedParent.getId());
        java.util.Map<String, ChecklistTaskInstance> tasksByKey = lockedTasks.stream()
                .collect(java.util.stream.Collectors.toMap(ChecklistTaskInstance::getTaskKey, task -> task));
        List<ChecklistDistributionItem> orderedItems = command.items().stream()
                .sorted(java.util.Comparator.comparing(item -> ChecklistDistributionKeyFactory.childKey(
                        lockedParent.getId(), item.templateItemVersionId())))
                .toList();

        for (ChecklistDistributionItem item : orderedItems) {
            String taskKey = ChecklistDistributionKeyFactory.childKey(instance.getId(), item.templateItemVersionId());
            ChecklistTaskInstance existing = tasksByKey.get(taskKey);
            if (existing == null && instance.getStatus() == ChecklistInstanceStatus.COMPLETED) {
                continue;
            }

            // Tính toán thời hạn Due Date theo mốc thai kỳ/sinh nở (Anchor + Offset)
            Instant dueAt = item.dueAnchor() == null || item.dueOffsetDays() == null
                    ? null
                    : eligibilityService.dueAt(item.dueAnchor(), command.lifecycleDates(),
                            item.dueOffsetDays(), item.dueOffsetUnit(), command.timezone());

            if (existing != null) {
                if (!matches(existing, instance, command, item, dueAt)) {
                    recordFailure(command, KEY_CONFLICT, existing.getId());
                    counters.conflicts++;
                    continue;
                }
                counters.existingTasks++;
                continue;
            }

            // Tạo mới ChecklistTaskInstance (Từng đầu việc cụ thể)
            ChecklistTaskInstance proposed = ChecklistTaskInstance.builder()
                    .checklistInstanceId(instance.getId())
                    .templateVersionId(command.templateVersionId())
                    .templateItemVersionId(item.templateItemVersionId())
                    .taskKey(taskKey)
                    .keyVersion(materializedKeyVersion(command))
                    .titleSnapshot(item.title())
                    .descriptionSnapshot(item.description())
                    .supportFunction(item.supportFunction())
                    .displayOrder(item.displayOrder())
                    .required(item.required())
                    .targetSubject(item.targetSubject())
                    .checklistContractVersion(materializedContractVersion(command))
                    .dueAt(dueAt)
                    .status(ChecklistTaskStatus.PENDING)
                    .build();
            try {
                // Lưu Task con vào cơ sở dữ liệu
                ChecklistTaskInstance created = taskRepository.save(proposed);
                counters.createdTasks++;

                // Ghi Audit Log gán đầu việc cần làm (CHECKLIST_ASSIGNED)
                auditWriter.write(event(AuditAction.CHECKLIST_ASSIGNED, command, recipient.userId(),
                        ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE, created.getId(),
                        created.getId(), null, ChecklistTaskStatus.PENDING.name(), null));
            } catch (DataIntegrityViolationException loser) {
                ChecklistTaskInstance winner = taskRepository.findByTaskKey(taskKey).orElseThrow(() -> loser);
                if (!matches(winner, instance, command, item, dueAt)) {
                    recordFailure(command, KEY_CONFLICT, winner.getId());
                    counters.conflicts++;
                    continue;
                }
                counters.existingTasks++;
            }
        }

        // Nếu là luồng Catch-up (bù lịch sử), tự động đóng instance sau khi materialize
        if (isCatchUp(command)) {
            closeCatchUpOccurrence(instance, command, recipient, counters);
        }
    }

    /**
     * Quét và chuyển các ChecklistInstance cũ đã hết hạn vào History hoặc Hủy (CANCELLED).
     */
    private void cancelObsoletePending(
            ChecklistDistributionCommand command,
            ChecklistDistributionRecipient recipient,
            ChecklistEligibilityDecision decision,
            Counters counters) {
        if (isPersonalMother(recipient)) {
            markObsoleteMotherHistory(command, recipient, decision, counters);
            return;
        }
        cancelObsoleteNonMotherPending(command, recipient, decision, counters);
    }

    /**
     * Đánh dấu các checklist thai kỳ/sau sinh cũ của Mẹ vào History khi chuyển sang tuần/giai đoạn mới.
     */
    private void markObsoleteMotherHistory(
            ChecklistDistributionCommand command,
            ChecklistDistributionRecipient recipient,
            ChecklistEligibilityDecision decision,
            Counters counters) {
        List<ChecklistInstance> prior = instanceRepository.findAllByLogicalPersonalIdentity(
                recipient.userId(), recipient.role(), command.contextType(), command.contextId(),
                command.templateVersionId(), ChecklistOrigin.SYSTEM_TEMPLATE);
        for (ChecklistInstance discovered : prior.stream()
                .sorted(java.util.Comparator.comparing(ChecklistInstance::getDistributionKey))
                .toList()) {
            ChecklistInstance instance = instanceRepository.findForUpdateById(discovered.getId())
                    .orElse(null);
            if (instance == null || instance.getStatus() == ChecklistInstanceStatus.CANCELLED
                    || !sameContract(instance, command)
                    || sameOccurrence(instance, command, decision)
                    || instance.getHistoricalAt() != null) {
                continue;
            }
            taskRepository.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(instance.getId());
            instance.setHistoricalAt(clock.instant());
            instance.setHistoryReasonCode(ChecklistHistoryReconciliationService.HISTORY_REASON_CODE);
            instanceRepository.save(instance);
            counters.cancelledInstances++;
        }
    }

    /**
     * Hủy bỏ các checklist pending của thành viên gia đình khi đợt việc đã lỗi thời.
     */
    private void cancelObsoleteNonMotherPending(
            ChecklistDistributionCommand command,
            ChecklistDistributionRecipient recipient,
            ChecklistEligibilityDecision decision,
            Counters counters) {
        List<ChecklistInstance> prior = instanceRepository
                .findAllByRecipientUserIdAndRecipientRoleAndCareGroupIdAndCareContextTypeAndCareContextIdAndTemplateVersionId(
                        recipient.userId(), recipient.role(), command.careGroupId(), command.contextType(),
                        command.contextId(), command.templateVersionId());
        for (ChecklistInstance discovered : prior.stream()
                .sorted(java.util.Comparator.comparing(ChecklistInstance::getDistributionKey))
                .toList()) {
            ChecklistInstance instance = instanceRepository.findForUpdateById(discovered.getId())
                    .orElse(null);
            if (instance == null
                    || instance.getOrigin() != ChecklistOrigin.SYSTEM_TEMPLATE
                    || instance.getStatus() != ChecklistInstanceStatus.PENDING
                    || !sameContract(instance, command)
                    || sameOccurrence(instance, command, decision)
                    || instance.getCompletedAt() != null
                    || instance.getCancelledAt() != null
                    || instance.getCancellationReasonCode() != null
                    || instance.getHistoricalAt() != null
                    || instance.getHistoryReasonCode() != null) {
                continue;
            }
            List<ChecklistTaskInstance> lockedTasks = taskRepository
                    .findAllForUpdateByChecklistInstanceIdOrderByTaskKey(instance.getId());
            if (lockedTasks.stream().anyMatch(task -> task.getStatus() != ChecklistTaskStatus.PENDING
                    || task.getCompletedAt() != null
                    || task.getSkippedAt() != null
                    || task.getCancelledAt() != null
                    || task.getActionReasonCode() != null)) {
                continue;
            }
            List<UUID> taskIds = lockedTasks.stream().map(ChecklistTaskInstance::getId).toList();
            if (!taskIds.isEmpty() && commandRepository.existsByTaskKindAndTaskIdIn("CHECKLIST", taskIds)) {
                continue;
            }
            Instant cancelledAt = clock.instant();
            for (ChecklistTaskInstance task : lockedTasks) {
                task.setStatus(ChecklistTaskStatus.CANCELLED);
                task.setCancelledAt(cancelledAt);
                task.setActionReasonCode(OBSOLETE);
                taskRepository.save(task);
                auditWriter.write(event(AuditAction.CHECKLIST_CANCELLED, command, recipient.userId(),
                        ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE, task.getId(), task.getId(),
                        ChecklistTaskStatus.PENDING.name(), ChecklistTaskStatus.CANCELLED.name(), OBSOLETE));
            }
            instance.setStatus(ChecklistInstanceStatus.CANCELLED);
            instance.setCancelledAt(cancelledAt);
            instance.setCancellationReasonCode(OBSOLETE);
            instanceRepository.save(instance);
            counters.cancelledInstances++;
            auditWriter.write(event(AuditAction.CHECKLIST_CANCELLED, command, recipient.userId(),
                    ChecklistAuditResourceType.CHECKLIST_INSTANCE, instance.getId(), null,
                    ChecklistInstanceStatus.PENDING.name(), ChecklistInstanceStatus.CANCELLED.name(), OBSOLETE));
        }
    }

    private boolean mayReceive(ChecklistDistributionCommand command, ChecklistDistributionRecipient recipient) {
        if (recipient == null || recipient.userId() == null || recipient.role() == null) {
            return false;
        }
        if (recipient.role() == ChecklistRecipientRole.MOTHER) {
            return recipient.userId().equals(command.careGroupOwnerUserId());
        }
        return command.careGroupId() != null && familyPermissionPolicy.canRead(
                recipient.acceptedMembership(), recipient.checklistView(), recipient.checklistComplete());
    }

    private static String lifecycleScopeKey(
            ChecklistDistributionCommand command,
            ChecklistDistributionRecipient recipient) {
        return ChecklistDistributionKeyFactory.lifecycleScopeKey(
                command.templateVersionId(), recipient.userId(), recipient.role().name(),
                recipientCareGroupId(command, recipient), command.contextType().name(), command.contextId());
    }

    private static boolean matches(
            ChecklistInstance instance,
            ChecklistDistributionCommand command,
            ChecklistDistributionRecipient recipient,
            ChecklistEligibilityDecision decision) {
        return sameOccurrenceIdentity(instance, command, recipient, decision)
                && sameCadence(instance, command);
    }

    private static boolean sameOccurrenceIdentity(
            ChecklistInstance instance,
            ChecklistDistributionCommand command,
            ChecklistDistributionRecipient recipient,
            ChecklistEligibilityDecision decision) {
        return Objects.equals(instance.getTemplateLineageId(), command.templateLineageId())
                && Objects.equals(instance.getTemplateVersionId(), command.templateVersionId())
                && Objects.equals(instance.getRecipientUserId(), recipient.userId())
                && instance.getRecipientRole() == recipient.role()
                && Objects.equals(instance.getCareGroupId(), recipientCareGroupId(command, recipient))
                && Objects.equals(instance.getCareGroupMemberId(),
                        recipient.role() == ChecklistRecipientRole.FAMILY
                                ? recipient.careGroupMemberId() : null)
                && Objects.equals(instance.getChecklistAccessEpoch(),
                        recipient.role() == ChecklistRecipientRole.FAMILY
                                ? recipient.checklistAccessEpoch() : null)
                && instance.getCareContextType() == command.contextType()
                && Objects.equals(instance.getCareContextId(), command.contextId())
                && Objects.equals(instance.getContextOwnerUserId(), command.contextOwnerUserId())
                && instance.getOrigin() == ChecklistOrigin.SYSTEM_TEMPLATE
                && Objects.equals(instance.getGestationalDatingRevision(), command.gestationalDatingRevision())
                && sameCadenceIdentity(instance, command.cadence(), command.checklistContractVersion())
                && sameWindow(instance, decision);
    }

    private static boolean matches(
            ChecklistTaskInstance task,
            ChecklistInstance instance,
            ChecklistDistributionCommand command,
            ChecklistDistributionItem item,
            Instant dueAt) {
        return Objects.equals(task.getChecklistInstanceId(), instance.getId())
                && Objects.equals(task.getTemplateVersionId(), command.templateVersionId())
                && Objects.equals(task.getTemplateItemVersionId(), item.templateItemVersionId())
                && Objects.equals(task.getTitleSnapshot(), item.title())
                && Objects.equals(task.getDescriptionSnapshot(), item.description())
                && task.getSupportFunction() == item.supportFunction()
                && Objects.equals(task.getDisplayOrder(), item.displayOrder())
                && Objects.equals(task.getRequired(), item.required())
                && task.getTargetSubject() == item.targetSubject()
                && contractsMatch(task.getChecklistContractVersion(), materializedContractVersion(command))
                && Objects.equals(task.getDueAt(), dueAt);
    }

    private static boolean sameCadence(
            ChecklistInstance instance,
            ChecklistDistributionCommand command) {
        ChecklistCadenceMetadata cadence = command.cadence();
        return sameCadenceIdentity(instance, cadence, command.checklistContractVersion())
                && (cadence == null
                    || (instance.getMaterializationMode() == cadence.materializationMode()
                        && Objects.equals(instance.getWasActionable(), cadence.wasActionable())));
    }

    /**
     * The root contract is meaningful even when a template has no recurring
     * cadence (for example, an optional V2 self-assigned recommendation).  A
     * null command contract remains the legacy V1 representation so old
     * callers keep their nullable database shape.
     */
    private static Short materializedContractVersion(ChecklistDistributionCommand command) {
        if (command.checklistContractVersion() != null) {
            return command.checklistContractVersion();
        }
        return command.cadence() == null ? null : (short) 2;
    }

    private static String materializedPeriodKey(ChecklistDistributionCommand command) {
        return command.cadence() == null
                ? (Short.valueOf(ChecklistPeriodIdentity.V2_CONTRACT_VERSION)
                        .equals(materializedContractVersion(command))
                        ? ChecklistPeriodIdentity.V2_NON_CADENCE_PERIOD_KEY : null)
                : command.cadence().periodKey();
    }

    private static String materializedScheduleZoneId(ChecklistDistributionCommand command) {
        return command.cadence() == null
                ? (Short.valueOf(ChecklistPeriodIdentity.V2_CONTRACT_VERSION)
                        .equals(materializedContractVersion(command))
                        ? ChecklistPeriodIdentity.V2_NON_CADENCE_ZONE_ID : null)
                : command.cadence().scheduleZone().getId();
    }

    private static ChecklistMaterializationMode materializedMode(ChecklistDistributionCommand command) {
        return command.cadence() == null
                ? (Short.valueOf(ChecklistPeriodIdentity.V2_CONTRACT_VERSION)
                        .equals(materializedContractVersion(command))
                        ? ChecklistPeriodIdentity.V2_NON_CADENCE_MODE : null)
                : command.cadence().materializationMode();
    }

    private static Boolean materializedWasActionable(ChecklistDistributionCommand command) {
        return command.cadence() == null
                ? (Short.valueOf(ChecklistPeriodIdentity.V2_CONTRACT_VERSION)
                        .equals(materializedContractVersion(command))
                        ? ChecklistPeriodIdentity.V2_NON_CADENCE_WAS_ACTIONABLE : null)
                : command.cadence().wasActionable();
    }

    private static String materializedKeyVersion(ChecklistDistributionCommand command) {
        // Cadence keys use the V2 identity format even for a V1 target-bearing
        // template (for example postpartum weekly work).  The discriminator
        // still records the template contract independently on the rows.
        return command.cadence() != null
                || Short.valueOf((short) 2).equals(materializedContractVersion(command))
                ? "v2" : "v1";
    }

    private static boolean contractsMatch(Short stored, Short expected) {
        if (expected == null || expected == 1) {
            return stored == null || stored == 1;
        }
        return Objects.equals(stored, expected);
    }

    private static boolean sameCadenceIdentity(
            ChecklistInstance instance,
            ChecklistCadenceMetadata cadence,
            Short contractVersion) {
        if (cadence == null) {
            if (Short.valueOf(ChecklistPeriodIdentity.V2_CONTRACT_VERSION)
                    .equals(contractVersion)) {
                return ChecklistPeriodIdentity.isV2NonCadenceIdentity(
                        instance.getChecklistContractVersion(),
                        instance.getPeriodKey(),
                        instance.getScheduleZoneId(),
                        instance.getMaterializationMode(),
                        instance.getWasActionable());
            }
            return instance.getPeriodKey() == null
                    && instance.getScheduleZoneId() == null
                    && contractsMatch(instance.getChecklistContractVersion(), contractVersion);
        }
        return Objects.equals(instance.getPeriodKey(), cadence.periodKey())
                && Objects.equals(instance.getScheduleZoneId(), cadence.scheduleZone().getId())
                && Objects.equals(instance.getChecklistContractVersion(),
                        contractVersion == null ? (short) 2 : contractVersion);
    }

    private static boolean isCatchUp(ChecklistDistributionCommand command) {
        return command.cadence() != null
                && command.cadence().materializationMode()
                    == com.carebridge.backend.checklist.model.ChecklistMaterializationMode.CATCH_UP;
    }

    private static boolean isCatchUpInstance(ChecklistInstance instance) {
        return instance.getMaterializationMode()
                == com.carebridge.backend.checklist.model.ChecklistMaterializationMode.CATCH_UP;
    }

    /**
     * Closes one missed cadence period in-place.  Existing completed work is
     * preserved; open work becomes CANCELLED and the parent is retained in
     * History with the controlled cadence-close reason.  The historical marker
     * is set before the transaction returns, so no action authorization can see
     * a half-open catch-up row.
     */
    private void closeCatchUpOccurrence(
            ChecklistInstance instance,
            ChecklistDistributionCommand command,
            ChecklistDistributionRecipient recipient,
            Counters counters) {
        if (instance.getHistoricalAt() != null) {
            return;
        }
        List<ChecklistTaskInstance> lockedTasks = taskRepository
                .findAllForUpdateByChecklistInstanceIdOrderByTaskKey(instance.getId());
        Instant closedAt = clock.instant();
        for (ChecklistTaskInstance task : lockedTasks) {
            ChecklistTaskStatus status = task.getStatus();
            if (status == ChecklistTaskStatus.COMPLETED
                    || status == ChecklistTaskStatus.SKIPPED
                    || status == ChecklistTaskStatus.CANCELLED) {
                continue;
            }
            String beforeStatus = status == null ? ChecklistTaskStatus.PENDING.name() : status.name();
            task.setStatus(ChecklistTaskStatus.CANCELLED);
            task.setCompletedAt(null);
            task.setSkippedAt(null);
            task.setCancelledAt(closedAt);
            task.setActionReasonCode(CADENCE_PERIOD_CLOSED);
            taskRepository.save(task);
            auditWriter.write(event(AuditAction.CHECKLIST_CANCELLED, command, recipient.userId(),
                    ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE, task.getId(), task.getId(),
                    beforeStatus, ChecklistTaskStatus.CANCELLED.name(), CADENCE_PERIOD_CLOSED));
        }
        boolean parentWasOpen = instance.getStatus() != ChecklistInstanceStatus.COMPLETED
                && instance.getStatus() != ChecklistInstanceStatus.CANCELLED;
        if (parentWasOpen) {
            String parentBeforeStatus = instance.getStatus() == null
                    ? ChecklistInstanceStatus.PENDING.name() : instance.getStatus().name();
            instance.setStatus(ChecklistInstanceStatus.CANCELLED);
            instance.setCompletedAt(null);
            instance.setCancelledAt(closedAt);
            instance.setCancellationReasonCode(CADENCE_PERIOD_CLOSED);
            auditWriter.write(event(AuditAction.CHECKLIST_CANCELLED, command, recipient.userId(),
                    ChecklistAuditResourceType.CHECKLIST_INSTANCE, instance.getId(), null,
                    parentBeforeStatus, ChecklistInstanceStatus.CANCELLED.name(),
                    CADENCE_PERIOD_CLOSED));
        }
        instance.setHistoricalAt(closedAt);
        instance.setHistoryReasonCode(CADENCE_PERIOD_CLOSED);
        instanceRepository.save(instance);
        counters.cancelledInstances++;
    }

    private static boolean sameWindow(ChecklistInstance instance, ChecklistEligibilityDecision decision) {
        return Objects.equals(instance.getWindowStart(), decision.windowStart())
                && Objects.equals(instance.getWindowEnd(), decision.windowEnd());
    }

    private static boolean sameOccurrence(
            ChecklistInstance instance,
            ChecklistDistributionCommand command,
            ChecklistEligibilityDecision decision) {
        return Objects.equals(instance.getGestationalDatingRevision(), command.gestationalDatingRevision())
                && sameWindow(instance, decision)
                && sameCadenceIdentity(instance, command.cadence(), command.checklistContractVersion());
    }

    private static boolean sameContract(ChecklistInstance instance, ChecklistDistributionCommand command) {
        return contractsMatch(instance.getChecklistContractVersion(), materializedContractVersion(command));
    }

    private static boolean isPersonalMother(ChecklistDistributionRecipient recipient) {
        return recipient != null
                && recipient.role() == ChecklistRecipientRole.MOTHER;
    }

    private static UUID recipientCareGroupId(
            ChecklistDistributionCommand command,
            ChecklistDistributionRecipient recipient) {
        return isPersonalMother(recipient) ? null : command.careGroupId();
    }

    private static ChecklistInstance canonicalPersonalInstance(List<ChecklistInstance> candidates) {
        return candidates.stream()
                .min(Comparator
                        .comparingInt((ChecklistInstance value) -> personalStatusRank(value.getStatus()))
                        .thenComparing(value -> value.getCareGroupId() != null)
                        .thenComparing(ChecklistInstance::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ChecklistInstance::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow();
    }

    private static int personalStatusRank(ChecklistInstanceStatus status) {
        if (status == ChecklistInstanceStatus.COMPLETED) return 0;
        if (status == ChecklistInstanceStatus.IN_PROGRESS) return 1;
        if (status == ChecklistInstanceStatus.PENDING) return 2;
        return 3;
    }

    private void recordFailure(ChecklistDistributionCommand command, String reason, UUID sourceId) {
        auditWriter.write(event(AuditAction.CHECKLIST_RECONCILIATION_FAILED, command,
                null, ChecklistAuditResourceType.MIGRATION_SOURCE, sourceId, null, null, null, reason));
    }

    private void auditFailure(ChecklistDistributionCommand command, String reason) {
        auditWriter.write(event(AuditAction.CHECKLIST_RECONCILIATION_FAILED, command,
                null, ChecklistAuditResourceType.CARE_CONTEXT, command.contextId(), null, null, null, reason));
    }

    private static ChecklistAuditEvent event(
            AuditAction action,
            ChecklistDistributionCommand command,
            UUID recipient,
            ChecklistAuditResourceType resourceType,
            UUID resourceId,
            UUID taskId,
            String before,
            String after,
            String reason) {
        return new ChecklistAuditEvent(action, null, ChecklistAuditActorType.SERVICE, DISTRIBUTOR,
                resourceType, resourceId, recipient, command.contextType(), command.contextId(),
                command.templateVersionId(), taskId, before, after, reason, command.correlationId());
    }

    private static void validateCommand(ChecklistDistributionCommand command) {
        Objects.requireNonNull(command, "Distribution command is required");
        Objects.requireNonNull(command.templateVersionId(), "Template version is required");
        Objects.requireNonNull(command.careGroupOwnerUserId(), "Care group owner is required");
        Objects.requireNonNull(command.contextType(), "Context type is required");
        Objects.requireNonNull(command.contextId(), "Context id is required");
        Objects.requireNonNull(command.contextOwnerUserId(), "Context owner is required");
        Objects.requireNonNull(command.correlationId(), "Correlation id is required");
        Objects.requireNonNull(command.recipients(), "Recipients are required");
        Objects.requireNonNull(command.items(), "Items are required");
        Objects.requireNonNull(command.timezone(), "Timezone is required");
        if (command.checklistContractVersion() != null
                && command.checklistContractVersion() != 1
                && command.checklistContractVersion() != 2) {
            throw new IllegalArgumentException("Unsupported checklist contract version");
        }
        if (command.cadence() != null
                && command.cadence().scheduleZone() == null) {
            throw new IllegalArgumentException("Cadence schedule zone is required");
        }
        if (command.checklistContractVersion() != null
                && command.checklistContractVersion() == 2
                && command.items().stream().anyMatch(item -> item.targetSubject() != null)) {
            throw new IllegalArgumentException("V2 checklist items must be targetless");
        }
        if (command.careGroupId() == null && command.recipients().stream()
                .filter(Objects::nonNull)
                .anyMatch(recipient -> recipient.role() != ChecklistRecipientRole.MOTHER)) {
            throw new IllegalArgumentException("A care group is required for non-mother recipients");
        }
        if (command.stage() == ContentStage.PREGNANCY
                && command.contextType() == ChecklistCareContextType.JOURNEY
                && (command.gestationalDatingRevision() == null
                || command.gestationalDatingRevision() <= 0)) {
            throw new IllegalArgumentException(
                    "Resolved pregnancy dating revision is required for checklist distribution");
        }
    }

    private static final class Counters {
        private int createdInstances;
        private int existingInstances;
        private int createdTasks;
        private int existingTasks;
        private int cancelledInstances;
        private int denied;
        private int conflicts;
        private int failures;

        private void add(Counters other) {
            createdInstances += other.createdInstances;
            existingInstances += other.existingInstances;
            createdTasks += other.createdTasks;
            existingTasks += other.existingTasks;
            cancelledInstances += other.cancelledInstances;
            denied += other.denied;
            conflicts += other.conflicts;
            failures += other.failures;
        }

        private ChecklistDistributionResult result() {
            return new ChecklistDistributionResult(createdInstances, existingInstances, createdTasks,
                    existingTasks, cancelledInstances, denied, conflicts, failures);
        }
    }
}
