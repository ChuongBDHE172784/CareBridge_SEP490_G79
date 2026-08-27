package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.repository.ChecklistActionCommandRepository;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ChecklistDistributionConcurrentLoserRecoveryTest {

    private static final UUID LINEAGE = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID VERSION = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID GROUP = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID OWNER = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID CONTEXT = UUID.fromString("10000000-0000-0000-0000-000000000005");
    private static final UUID ITEM = UUID.fromString("10000000-0000-0000-0000-000000000006");

    @Test
    void chk018_parentUniqueLoserRefetchesAndReusesPersistedWinner() {
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistInstance winner = matchingInstance();
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty(), Optional.of(winner));
        when(instances.save(any())).thenThrow(new DataIntegrityViolationException("parent unique loser"));

        ChecklistDistributionResult result = service(instances, tasks).distribute(command(List.of()));

        assertThat(result.existingInstances()).isEqualTo(1);
        assertThat(result.createdInstances()).isZero();
    }

    @Test
    void chk018_childUniqueLoserRefetchesAndReusesPersistedWinner() {
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistInstance parent = matchingInstance();
        ChecklistTaskInstance child = ChecklistTaskInstance.builder()
                .id(UUID.fromString("10000000-0000-0000-0000-000000000008"))
                .checklistInstanceId(parent.getId())
                .templateVersionId(VERSION)
                .templateItemVersionId(ITEM)
                .titleSnapshot("Hydrate")
                .displayOrder(1)
                .required(true)
                .targetSubject(ChecklistTargetSubject.MOTHER)
                .build();
        when(instances.findByDistributionKey(any())).thenReturn(Optional.of(parent));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(parent.getId()))
                .thenReturn(List.of());
        when(tasks.findByTaskKey(any())).thenReturn(Optional.of(child));
        when(tasks.save(any())).thenThrow(new DataIntegrityViolationException("child unique loser"));

        ChecklistDistributionResult result = service(instances, tasks).distribute(command(List.of(
                new ChecklistDistributionItem(ITEM, "Hydrate", 1, true,
                        ChecklistTargetSubject.MOTHER, null, null))));

        assertThat(result.existingTasks()).isEqualTo(1);
        assertThat(result.createdTasks()).isZero();
    }

    private static ChecklistDistributionService service(
            ChecklistInstanceRepository instances,
            ChecklistTaskInstanceRepository tasks) {
        return new ChecklistDistributionService(
                instances,
                tasks,
                mock(ChecklistActionCommandRepository.class),
                mock(ChecklistAuditWriter.class),
                new ChecklistLifecycleEligibilityService(),
                Clock.systemUTC());
    }

    private static ChecklistDistributionCommand command(List<ChecklistDistributionItem> items) {
        return new ChecklistDistributionCommand(
                LINEAGE,
                VERSION,
                GROUP,
                OWNER,
                ChecklistCareContextType.JOURNEY,
                CONTEXT,
                OWNER,
                null,
                null,
                new ChecklistLifecycleDates(null, null, null, null),
                LocalDate.of(2026, 7, 29),
                ZoneOffset.UTC,
                List.of(new ChecklistDistributionRecipient(
                        OWNER, ChecklistRecipientRole.MOTHER, true, true, true)),
                items,
                UUID.fromString("10000000-0000-0000-0000-000000000009"));
    }

    private static ChecklistInstance matchingInstance() {
        return ChecklistInstance.builder()
                .id(UUID.fromString("10000000-0000-0000-0000-000000000007"))
                .templateLineageId(LINEAGE)
                .templateVersionId(VERSION)
                .recipientUserId(OWNER)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careGroupId(null)
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(CONTEXT)
                .contextOwnerUserId(OWNER)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .status(ChecklistInstanceStatus.PENDING)
                .build();
    }
}
