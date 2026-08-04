package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

class UnifiedTaskPolicyTest {

    private static final UUID MOTHER = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID FAMILY = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID GROUP = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID CONTEXT = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @Test
    void accessPolicyIsRegisteredForRuntimeTodayProviders() {
        assertThat(UnifiedTaskAccessPolicy.class.isAnnotationPresent(Component.class)).isTrue();
    }

    @Test
    void chk014_familyPermissionTruthTableIsDefaultDeny() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy familyPolicy = mock(CareGroupAuthorizationPolicy.class);
        when(groups.findById(GROUP)).thenReturn(Optional.of(CareGroup.builder()
                .id(GROUP).ownerUserId(MOTHER).linkedJourneyId(CONTEXT)
                .status(CareGroupStatus.ACTIVE).build()));
        UnifiedTaskAccessPolicy policy = canonicalPolicy(groups, familyPolicy);
        ChecklistInstance instance = familyInstance();

        assertThat(policy.canView(instance, FAMILY)).isFalse();
        assertThat(policy.canComplete(instance, FAMILY)).isFalse();

        when(familyPolicy.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        assertThat(policy.canView(instance, FAMILY)).isTrue();
        assertThat(policy.canComplete(instance, FAMILY)).isFalse();

        when(familyPolicy.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(false);
        when(familyPolicy.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_COMPLETE)).thenReturn(true);
        assertThat(policy.canView(instance, FAMILY)).isFalse();
        assertThat(policy.canComplete(instance, FAMILY)).isFalse();

        when(familyPolicy.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        assertThat(policy.canView(instance, FAMILY)).isTrue();
        assertThat(policy.canComplete(instance, FAMILY)).isTrue();
    }

    @Test
    void chk017_contextOwnerMismatchAndOtherRecipientAreIndistinguishableDenials() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy familyPolicy = mock(CareGroupAuthorizationPolicy.class);
        when(groups.findById(GROUP)).thenReturn(Optional.of(CareGroup.builder()
                .id(GROUP).ownerUserId(UUID.randomUUID()).linkedJourneyId(CONTEXT)
                .status(CareGroupStatus.ACTIVE).build()));
        when(familyPolicy.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        UnifiedTaskAccessPolicy policy = canonicalPolicy(groups, familyPolicy);

        assertThat(policy.canView(familyInstance(), FAMILY)).isFalse();
        assertThat(policy.canView(familyInstance(), UUID.randomUUID())).isFalse();
    }

    @Test
    void personalMotherCanAccessOwnedActiveJourneyWithoutCareGroup() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy familyPolicy = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        when(journeys.existsByIdAndOwnerUserIdAndStatus(CONTEXT, MOTHER, JourneyStatus.ACTIVE))
                .thenReturn(true);
        UnifiedTaskAccessPolicy policy = new UnifiedTaskAccessPolicy(
                groups, familyPolicy, journeys, babies);
        ChecklistInstance instance = ChecklistInstance.builder()
                .recipientUserId(MOTHER)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careGroupId(null)
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(CONTEXT)
                .contextOwnerUserId(MOTHER)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .build();

        assertThat(policy.canView(instance, MOTHER)).isTrue();
        assertThat(policy.canComplete(instance, MOTHER)).isTrue();
        assertThat(policy.canView(instance, FAMILY)).isFalse();
    }

    @Test
    void familyCanViewCanonicalMotherTemplateOnlyThroughExplicitGroupScope() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy familyPolicy = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        when(groups.findById(GROUP)).thenReturn(Optional.of(CareGroup.builder()
                .id(GROUP).ownerUserId(MOTHER).linkedJourneyId(CONTEXT)
                .status(CareGroupStatus.ACTIVE).build()));
        when(journeys.existsByIdAndOwnerUserIdAndStatus(CONTEXT, MOTHER, JourneyStatus.ACTIVE))
                .thenReturn(true);
        when(familyPolicy.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW))
                .thenReturn(true);
        UnifiedTaskAccessPolicy policy = new UnifiedTaskAccessPolicy(
                groups, familyPolicy, journeys, babies);
        ChecklistInstance instance = ChecklistInstance.builder()
                .recipientUserId(MOTHER)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careGroupId(null)
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(CONTEXT)
                .contextOwnerUserId(MOTHER)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .build();

        assertThat(policy.canView(instance, FAMILY, GROUP)).isTrue();
        assertThat(policy.canComplete(instance, FAMILY, GROUP)).isFalse();
        when(familyPolicy.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_COMPLETE))
                .thenReturn(true);
        assertThat(policy.canComplete(instance, FAMILY, GROUP)).isTrue();

        instance.setOrigin(ChecklistOrigin.USER_CREATED);
        assertThat(policy.canView(instance, FAMILY, GROUP)).isFalse();
    }

    @Test
    void activeCanonicalGroupContextDoesNotRequireReviewedMapping() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy familyPolicy = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        when(groups.findById(GROUP)).thenReturn(Optional.of(CareGroup.builder()
                .id(GROUP).ownerUserId(MOTHER).linkedJourneyId(CONTEXT)
                .status(CareGroupStatus.ACTIVE).build()));
        when(journeys.existsByIdAndOwnerUserIdAndStatus(CONTEXT, MOTHER, JourneyStatus.ACTIVE))
                .thenReturn(true);
        when(familyPolicy.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        UnifiedTaskAccessPolicy policy = new UnifiedTaskAccessPolicy(
                groups, familyPolicy, journeys, babies);

        assertThat(policy.canView(familyInstance(), FAMILY)).isTrue();

        when(journeys.existsByIdAndOwnerUserIdAndStatus(CONTEXT, MOTHER, JourneyStatus.ACTIVE))
                .thenReturn(false);
        assertThat(policy.canView(familyInstance(), FAMILY)).isFalse();
    }

    @Test
    void chk029And030_userTaskRequiresTargetAndSystemTaskIsImmutable() {
        UnifiedTaskMutationPolicy policy = new UnifiedTaskMutationPolicy();

        assertThatThrownBy(() -> policy.requireUserCreatedTarget(ChecklistOrigin.USER_CREATED, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("ITEM_TARGET_REQUIRED"));
        policy.requireUserCreatedTarget(ChecklistOrigin.USER_CREATED, ChecklistTargetSubject.BABY);

        assertThatThrownBy(() -> policy.requireMutable(ChecklistOrigin.SYSTEM_TEMPLATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("SYSTEM_TASK_IMMUTABLE"));
        policy.requireMutable(ChecklistOrigin.USER_CREATED);
    }

    private static ChecklistInstance familyInstance() {
        return ChecklistInstance.builder()
                .recipientUserId(FAMILY)
                .recipientRole(ChecklistRecipientRole.FAMILY)
                .careGroupId(GROUP)
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(CONTEXT)
                .contextOwnerUserId(MOTHER)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .build();
    }

    private static UnifiedTaskAccessPolicy canonicalPolicy(
            CareGroupRepository groups,
            CareGroupAuthorizationPolicy familyPolicy) {
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        when(journeys.existsByIdAndOwnerUserIdAndStatus(CONTEXT, MOTHER, JourneyStatus.ACTIVE))
                .thenReturn(true);
        return new UnifiedTaskAccessPolicy(groups, familyPolicy, journeys, babies);
    }
}
