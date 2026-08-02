package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecklistHistoryReconciliationService {

    public static final String HISTORY_REASON_CODE = "LIFECYCLE_STAGE_OBSOLETE";

    private final ChecklistInstanceRepository instanceRepository;
    private final ChecklistTaskInstanceRepository taskRepository;
    private final ChecklistCurrentScopePolicy currentScopePolicy;
    private final Clock clock;

    @Autowired
    public ChecklistHistoryReconciliationService(
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            ChecklistCurrentScopePolicy currentScopePolicy) {
        this(instanceRepository, taskRepository, currentScopePolicy, Clock.systemUTC());
    }

    public ChecklistHistoryReconciliationService(
            ChecklistInstanceRepository instanceRepository,
            ChecklistTaskInstanceRepository taskRepository,
            ChecklistCurrentScopePolicy currentScopePolicy,
            Clock clock) {
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.currentScopePolicy = currentScopePolicy;
        this.clock = clock;
    }

    @Transactional
    public int reconcile(UUID ownerUserId, LocalDate effectiveDate, ZoneId timezone, UUID correlationId) {
        if (ownerUserId == null || effectiveDate == null || timezone == null || correlationId == null) {
            return 0;
        }
        List<ChecklistInstance> candidates = instanceRepository
                .findByContextOwnerUserIdAndRecipientRoleAndOriginAndHistoricalAtIsNull(
                        ownerUserId, ChecklistRecipientRole.MOTHER, ChecklistOrigin.SYSTEM_TEMPLATE)
                .stream()
                .filter(instance -> Objects.equals(ownerUserId, instance.getRecipientUserId()))
                .filter(currentScopePolicy::isHistoryManaged)
                .sorted(Comparator.comparing(ChecklistHistoryReconciliationService::lifecycleKey)
                        .thenComparing(ChecklistInstance::getId))
                .toList();

        int marked = 0;
        Instant now = clock.instant();
        for (ChecklistInstance candidate : candidates) {
            if (currentScopePolicy.isCurrent(candidate, effectiveDate)) {
                continue;
            }
            instanceRepository.acquireDistributionKeyLock(lifecycleKey(candidate));
            ChecklistInstance locked = instanceRepository.findForUpdateById(candidate.getId()).orElse(null);
            if (locked == null
                    || locked.getHistoricalAt() != null
                    || !currentScopePolicy.isHistoryManaged(locked)
                    || currentScopePolicy.isCurrent(locked, effectiveDate)) {
                continue;
            }
            taskRepository.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(locked.getId());
            locked.setHistoricalAt(now);
            locked.setHistoryReasonCode(HISTORY_REASON_CODE);
            instanceRepository.save(locked);
            marked++;
        }
        return marked;
    }

    private static String lifecycleKey(ChecklistInstance instance) {
        return ChecklistDistributionKeyFactory.lifecycleScopeKey(
                instance.getTemplateVersionId(), instance.getRecipientUserId(),
                instance.getRecipientRole().name(), instance.getCareGroupId(),
                instance.getCareContextType().name(), instance.getCareContextId());
    }
}
