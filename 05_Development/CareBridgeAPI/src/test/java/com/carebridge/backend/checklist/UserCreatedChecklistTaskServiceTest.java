package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.dto.AddChecklistItemRequest;
import com.carebridge.backend.checklist.entity.ChecklistCategory;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.service.UserCreatedChecklistTaskService;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserCreatedChecklistTaskServiceTest {

    @Test
    void createsPersonalTaskWithoutReusingALegacyGroupScopedParent() {
        UUID actor = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        UUID clientTaskId = UUID.randomUUID();
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        UnifiedTaskMutationPolicy mutationPolicy = mock(UnifiedTaskMutationPolicy.class);
        UnifiedTaskAccessPolicy accessPolicy = mock(UnifiedTaskAccessPolicy.class);
        AuditService audit = mock(AuditService.class);
        UserCreatedChecklistTaskService service = new UserCreatedChecklistTaskService(
                journeys, babies, instances, tasks, mutationPolicy, accessPolicy, audit);
        when(journeys.findByIdAndOwnerUserIdAndStatus(journeyId, actor, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(MotherJourney.builder().id(journeyId).ownerUserId(actor)
                        .journeyType(JourneyType.PREGNANCY).status(JourneyStatus.ACTIVE).build()));
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());
        when(instances.findAllByLogicalPersonalIdentity(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(ChecklistInstance.builder()
                        .id(UUID.randomUUID())
                        .careGroupId(UUID.randomUUID())
                        .status(ChecklistInstanceStatus.PENDING)
                        .build()));
        when(instances.saveAndFlush(any())).thenAnswer(invocation -> {
            ChecklistInstance value = invocation.getArgument(0);
            value.setId(UUID.randomUUID());
            return value;
        });
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(any())).thenReturn(List.of());
        when(tasks.saveAndFlush(any())).thenAnswer(invocation -> {
            ChecklistTaskInstance value = invocation.getArgument(0);
            value.setId(UUID.randomUUID());
            return value;
        });
        AddChecklistItemRequest request = new AddChecklistItemRequest(
                journeyId, null, "Drink water", ChecklistCategory.GENERAL, 0,
                ChecklistTargetSubject.MOTHER, clientTaskId);

        var response = service.create(request, actor);

        ArgumentCaptor<ChecklistInstance> parent = ArgumentCaptor.forClass(ChecklistInstance.class);
        verify(instances).saveAndFlush(parent.capture());
        assertThat(parent.getValue().getCareGroupId()).isNull();
        assertThat(parent.getValue().getOrigin()).isEqualTo(ChecklistOrigin.USER_CREATED);
        assertThat(response.journeyId()).isEqualTo(journeyId);
        assertThat(response.itemText()).isEqualTo("Drink water");
        var lockOrder = inOrder(instances, tasks);
        lockOrder.verify(instances).acquireDistributionKeyLock(
                ChecklistDistributionKeyFactory.lifecycleScopeKey(
                        null, actor, "MOTHER", null, "JOURNEY", journeyId));
        lockOrder.verify(instances).saveAndFlush(any());
        lockOrder.verify(tasks).findAllForUpdateByChecklistInstanceIdOrderByTaskKey(any());
    }
}
