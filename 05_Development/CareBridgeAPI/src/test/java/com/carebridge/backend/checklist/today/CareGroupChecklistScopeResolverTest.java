package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.today.policy.CareGroupChecklistScopeResolver;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareGroupChecklistScopeResolverTest {
    private static final UUID MOTHER = UUID.fromString("41000000-0000-0000-0000-000000000001");
    private static final UUID FAMILY = UUID.fromString("41000000-0000-0000-0000-000000000002");
    private static final UUID GROUP = UUID.fromString("41000000-0000-0000-0000-000000000003");
    private static final UUID JOURNEY = UUID.fromString("41000000-0000-0000-0000-000000000004");

    @Test
    void resolvesOnlyAnExplicitAcceptedGroupWithAnActiveLinkedJourney() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy permissions = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        when(groups.findByIdAndStatus(GROUP, CareGroupStatus.ACTIVE)).thenReturn(Optional.of(
                CareGroup.builder().id(GROUP).ownerUserId(MOTHER).groupName("Family")
                        .linkedJourneyId(JOURNEY).status(CareGroupStatus.ACTIVE).build()));
        when(permissions.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        when(journeys.existsByIdAndOwnerUserIdAndStatus(JOURNEY, MOTHER, JourneyStatus.ACTIVE))
                .thenReturn(true);

        var scope = new CareGroupChecklistScopeResolver(groups, permissions, journeys, babies)
                .resolveView(FAMILY, GROUP);

        assertThat(scope).isNotNull();
        assertThat(scope.ownerUserId()).isEqualTo(MOTHER);
        assertThat(scope.includes(ChecklistCareContextType.JOURNEY, JOURNEY)).isTrue();
        assertThat(scope.includes(ChecklistCareContextType.JOURNEY, UUID.randomUUID())).isFalse();
    }

    @Test
    void missingViewPermissionDoesNotFallBackToAnotherGroup() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy permissions = mock(CareGroupAuthorizationPolicy.class);
        when(groups.findByIdAndStatus(GROUP, CareGroupStatus.ACTIVE)).thenReturn(Optional.of(
                CareGroup.builder().id(GROUP).ownerUserId(MOTHER).linkedJourneyId(JOURNEY)
                        .status(CareGroupStatus.ACTIVE).build()));
        when(permissions.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(false);

        var scope = new CareGroupChecklistScopeResolver(groups, permissions).resolveView(FAMILY, GROUP);

        assertThat(scope).isNull();
    }
}
