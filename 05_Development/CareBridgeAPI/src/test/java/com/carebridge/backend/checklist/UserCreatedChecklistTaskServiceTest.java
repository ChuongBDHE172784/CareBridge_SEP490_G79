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
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.checklist.service.UserCreatedChecklistTaskService;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
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

    @Test
    void createsPrivateFamilyTaskInsideTheSelectedGroupContext() {
        UUID actor = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        UUID clientTaskId = UUID.randomUUID();
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        UnifiedTaskMutationPolicy mutationPolicy = mock(UnifiedTaskMutationPolicy.class);
        UnifiedTaskAccessPolicy accessPolicy = mock(UnifiedTaskAccessPolicy.class);
        AuditService audit = mock(AuditService.class);
        ChecklistTemplateRepository templates = mock(ChecklistTemplateRepository.class);
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupMemberRepository members = mock(CareGroupMemberRepository.class);
        CareGroupAuthorizationPolicy permissions = mock(CareGroupAuthorizationPolicy.class);
        UserCreatedChecklistTaskService service = new UserCreatedChecklistTaskService(
                journeys, babies, instances, tasks, mutationPolicy, accessPolicy, audit, templates,
                groups, members, permissions);

        when(groups.findByIdAndStatus(groupId, CareGroupStatus.ACTIVE)).thenReturn(Optional.of(
                CareGroup.builder().id(groupId).ownerUserId(owner).status(CareGroupStatus.ACTIVE)
                        .linkedJourneyId(journeyId).build()));
        when(members.findByCareGroupIdAndUserId(groupId, actor)).thenReturn(Optional.of(
                CareGroupMember.builder().id(UUID.randomUUID()).careGroupId(groupId).userId(actor)
                        .checklistAccessEpoch(0L)
                        .memberRole(GroupMemberRole.MEMBER).inviteStatus(InviteStatus.ACCEPTED).build()));
        when(permissions.hasPermission(groupId, actor,
                com.carebridge.backend.family.entity.PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        when(journeys.findByIdAndOwnerUserIdAndStatus(journeyId, owner, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(MotherJourney.builder().id(journeyId).ownerUserId(owner)
                        .journeyType(JourneyType.PREGNANCY).status(JourneyStatus.ACTIVE).build()));
        when(instances.findByDistributionKey(any())).thenReturn(Optional.empty());
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

        var response = service.create(new AddChecklistItemRequest(
                null, null, "Nhắc mẹ uống nước", ChecklistCategory.GENERAL, 0,
                ChecklistTargetSubject.MOTHER, clientTaskId, groupId), actor);

        ArgumentCaptor<ChecklistInstance> parent = ArgumentCaptor.forClass(ChecklistInstance.class);
        verify(instances).saveAndFlush(parent.capture());
        assertThat(parent.getValue().getRecipientRole())
                .isEqualTo(com.carebridge.backend.checklist.model.ChecklistRecipientRole.FAMILY);
        assertThat(parent.getValue().getRecipientUserId()).isEqualTo(actor);
        assertThat(parent.getValue().getCareGroupId()).isEqualTo(groupId);
        assertThat(parent.getValue().getCareGroupMemberId()).isNotNull();
        assertThat(parent.getValue().getChecklistAccessEpoch()).isZero();
        assertThat(parent.getValue().getContextOwnerUserId()).isEqualTo(owner);
        assertThat(parent.getValue().getCareContextId()).isEqualTo(journeyId);
        assertThat(response.journeyId()).isEqualTo(journeyId);
    }

    @Test
    void revivesCancelledPersonalParentOnlyWhenCreatingANewChild() {
        UUID actor = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        UUID clientTaskId = UUID.randomUUID();
        ChecklistInstance parent = ChecklistInstance.builder()
                .id(UUID.randomUUID())
                .recipientUserId(actor)
                .recipientRole(com.carebridge.backend.checklist.model.ChecklistRecipientRole.MOTHER)
                .careGroupId(null)
                .careContextType(com.carebridge.backend.checklist.model.ChecklistCareContextType.JOURNEY)
                .careContextId(journeyId)
                .contextOwnerUserId(actor)
                .origin(ChecklistOrigin.USER_CREATED)
                .status(ChecklistInstanceStatus.CANCELLED)
                .completedAt(null)
                .cancelledAt(java.time.Instant.parse("2026-08-01T08:00:00Z"))
                .cancellationReasonCode("USER_DELETED")
                .build();
        ChecklistTaskInstance cancelledChild = ChecklistTaskInstance.builder()
                .id(UUID.randomUUID())
                .checklistInstanceId(parent.getId())
                .taskKey("cancelled-child")
                .status(ChecklistTaskStatus.CANCELLED)
                .build();
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
        when(instances.findByDistributionKey(any())).thenReturn(Optional.of(parent));
        when(tasks.findAllForUpdateByChecklistInstanceIdOrderByTaskKey(parent.getId()))
                .thenReturn(List.of(cancelledChild));
        when(tasks.saveAndFlush(any())).thenAnswer(invocation -> {
            ChecklistTaskInstance value = invocation.getArgument(0);
            value.setId(UUID.randomUUID());
            return value;
        });
        AddChecklistItemRequest request = new AddChecklistItemRequest(
                journeyId, null, "New task", ChecklistCategory.GENERAL, 0,
                ChecklistTargetSubject.MOTHER, clientTaskId);

        var response = service.create(request, actor);

        assertThat(response.itemText()).isEqualTo("New task");
        assertThat(parent.getStatus()).isEqualTo(ChecklistInstanceStatus.PENDING);
        assertThat(parent.getCompletedAt()).isNull();
        assertThat(parent.getCancelledAt()).isNull();
        assertThat(parent.getCancellationReasonCode()).isNull();
        verify(instances).save(parent);
    }

    @Test
    void listAuthorizedOmitsArchivedSystemTemplateInstances() {
        UUID actor = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        ChecklistInstance archived = ChecklistInstance.builder()
                .id(instanceId)
                .templateVersionId(versionId)
                .recipientUserId(actor)
                .recipientRole(com.carebridge.backend.checklist.model.ChecklistRecipientRole.MOTHER)
                .careContextType(com.carebridge.backend.checklist.model.ChecklistCareContextType.JOURNEY)
                .careContextId(UUID.randomUUID())
                .contextOwnerUserId(actor)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .status(ChecklistInstanceStatus.PENDING)
                .build();
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        UnifiedTaskMutationPolicy mutationPolicy = mock(UnifiedTaskMutationPolicy.class);
        UnifiedTaskAccessPolicy accessPolicy = mock(UnifiedTaskAccessPolicy.class);
        AuditService audit = mock(AuditService.class);
        ChecklistTemplateRepository templates = mock(ChecklistTemplateRepository.class);
        when(instances.findByRecipientUserId(actor)).thenReturn(List.of(archived));
        when(templates.findAllByTemplateVersionIdIn(List.of(versionId))).thenReturn(List.of(
                ChecklistTemplate.builder().templateVersionId(versionId)
                        .status(ChecklistTemplateStatus.ARCHIVED).build()));

        UserCreatedChecklistTaskService service = new UserCreatedChecklistTaskService(
                journeys, babies, instances, tasks, mutationPolicy, accessPolicy, audit, templates);

        assertThat(service.listAuthorized(actor, null, null)).isEmpty();
        verify(tasks, org.mockito.Mockito.never()).findAllByChecklistInstanceIds(any());
    }
}
