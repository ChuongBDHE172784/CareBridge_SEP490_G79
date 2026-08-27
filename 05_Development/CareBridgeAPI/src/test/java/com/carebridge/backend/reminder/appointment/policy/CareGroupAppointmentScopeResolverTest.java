package com.carebridge.backend.reminder.appointment.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
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

class CareGroupAppointmentScopeResolverTest {

    private static final UUID MOTHER = UUID.fromString("52000000-0000-0000-0000-000000000001");
    private static final UUID FAMILY = UUID.fromString("52000000-0000-0000-0000-000000000002");
    private static final UUID GROUP = UUID.fromString("52000000-0000-0000-0000-000000000003");
    private static final UUID JOURNEY = UUID.fromString("52000000-0000-0000-0000-000000000004");

    @Test
    void calendarPermissionAndActiveLinkedJourneyAreRequired() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy permissions = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        when(groups.findByIdAndStatus(GROUP, CareGroupStatus.ACTIVE)).thenReturn(Optional.of(
                CareGroup.builder().id(GROUP).ownerUserId(MOTHER).linkedJourneyId(JOURNEY)
                        .status(CareGroupStatus.ACTIVE).build()));
        when(permissions.hasPermission(GROUP, FAMILY, PermissionFlag.CALENDAR)).thenReturn(true);
        when(journeys.existsByIdAndOwnerUserIdAndStatus(JOURNEY, MOTHER, JourneyStatus.ACTIVE))
                .thenReturn(true);

        var scope = new CareGroupAppointmentScopeResolver(groups, permissions, journeys, babies)
                .resolveView(FAMILY, GROUP);

        assertThat(scope).isNotNull();
        assertThat(scope.ownerUserId()).isEqualTo(MOTHER);
        assertThat(scope.linkedJourneyId()).isEqualTo(JOURNEY);
        assertThat(scope.includes(JOURNEY, null)).isTrue();
    }

    @Test
    void missingCalendarPermissionFailsClosed() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy permissions = mock(CareGroupAuthorizationPolicy.class);
        when(groups.findByIdAndStatus(GROUP, CareGroupStatus.ACTIVE)).thenReturn(Optional.of(
                CareGroup.builder().id(GROUP).ownerUserId(MOTHER).linkedJourneyId(JOURNEY)
                        .status(CareGroupStatus.ACTIVE).build()));
        when(permissions.hasPermission(GROUP, FAMILY, PermissionFlag.CALENDAR)).thenReturn(false);

        var scope = new CareGroupAppointmentScopeResolver(groups, permissions,
                mock(MotherJourneyRepository.class), mock(BabyProfileRepository.class))
                .resolveView(FAMILY, GROUP);

        assertThat(scope).isNull();
    }
}
