package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.today.policy.ReminderAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.reminder.entity.Reminder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReminderAccessPolicyTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID FAMILY = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID JOURNEY = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID BABY = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID GROUP = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @Test
    void ownerRetainsScopedAndUnscopedReminderAccess() {
        Fixture fixture = fixture();
        Reminder unscoped = Reminder.builder().ownerUserId(OWNER).build();
        Reminder scoped = Reminder.builder().ownerUserId(OWNER).journeyId(JOURNEY).build();

        assertThat(fixture.policy().canView(unscoped, OWNER)).isTrue();
        assertThat(fixture.policy().canComplete(unscoped, OWNER)).isTrue();
        assertThat(fixture.policy().canView(scoped, OWNER)).isTrue();
        assertThat(fixture.policy().canComplete(scoped, OWNER)).isTrue();
    }

    @Test
    void familyRequiresUniqueActiveOwnerContextGroupAndBothPermissionsForAction() {
        Fixture fixture = fixture();
        Reminder reminder = Reminder.builder().ownerUserId(OWNER).journeyId(JOURNEY).build();
        CareGroup group = activeGroup(GROUP);
        when(fixture.groups().findByLinkedJourneyId(JOURNEY)).thenReturn(List.of(group));
        when(fixture.journeys().existsByIdAndOwnerUserId(JOURNEY, OWNER)).thenReturn(true);
        when(fixture.familyAuthorization().hasPermission(
                GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        when(fixture.familyAuthorization().hasPermission(
                GROUP, FAMILY, PermissionFlag.CHECKLIST_COMPLETE)).thenReturn(true);

        assertThat(fixture.policy().canView(reminder, FAMILY)).isTrue();
        assertThat(fixture.policy().canComplete(reminder, FAMILY)).isTrue();
        assertThat(fixture.policy().presentationGroup(reminder, FAMILY)).contains(group);
    }

    @Test
    void familyIsDeniedWhenContextGroupIsAmbiguousOrCompletePermissionIsMissing() {
        Fixture fixture = fixture();
        Reminder reminder = Reminder.builder().ownerUserId(OWNER).journeyId(JOURNEY).build();
        CareGroup first = activeGroup(GROUP);
        CareGroup second = activeGroup(UUID.fromString("00000000-0000-0000-0000-000000000302"));
        when(fixture.groups().findByLinkedJourneyId(JOURNEY)).thenReturn(List.of(first, second));
        when(fixture.journeys().existsByIdAndOwnerUserId(JOURNEY, OWNER)).thenReturn(true);

        assertThat(fixture.policy().canView(reminder, FAMILY)).isFalse();
        assertThat(fixture.policy().canComplete(reminder, FAMILY)).isFalse();

        when(fixture.groups().findByLinkedJourneyId(JOURNEY)).thenReturn(List.of(first));
        when(fixture.familyAuthorization().hasPermission(
                GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        when(fixture.familyAuthorization().hasPermission(
                GROUP, FAMILY, PermissionFlag.CHECKLIST_COMPLETE)).thenReturn(false);
        assertThat(fixture.policy().canView(reminder, FAMILY)).isTrue();
        assertThat(fixture.policy().canComplete(reminder, FAMILY)).isFalse();
    }

    @Test
    void familyIsDeniedWhenJourneyBelongsToAnotherOwnerDespiteMatchingGroupAndReminderOwners() {
        Fixture fixture = fixture();
        Reminder reminder = Reminder.builder().ownerUserId(OWNER).journeyId(JOURNEY).build();
        CareGroup group = activeGroup(GROUP);
        when(fixture.groups().findByLinkedJourneyId(JOURNEY)).thenReturn(List.of(group));
        when(fixture.journeys().existsByIdAndOwnerUserId(JOURNEY, OWNER)).thenReturn(false);
        when(fixture.familyAuthorization().hasPermission(
                GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        when(fixture.familyAuthorization().hasPermission(
                GROUP, FAMILY, PermissionFlag.CHECKLIST_COMPLETE)).thenReturn(true);

        assertThat(fixture.policy().canView(reminder, FAMILY)).isFalse();
        assertThat(fixture.policy().canComplete(reminder, FAMILY)).isFalse();
        assertThat(fixture.policy().presentationGroup(reminder, FAMILY)).isEmpty();
    }

    @Test
    void familyIsDeniedWhenBabyBelongsToAnotherOwnerDespiteMatchingGroupAndReminderOwners() {
        Fixture fixture = fixture();
        Reminder reminder = Reminder.builder().ownerUserId(OWNER).babyId(BABY).build();
        CareGroup group = CareGroup.builder()
                .id(GROUP)
                .ownerUserId(OWNER)
                .linkedBabyProfileId(BABY)
                .status(CareGroupStatus.ACTIVE)
                .build();
        when(fixture.groups().findByLinkedBabyProfileId(BABY)).thenReturn(List.of(group));
        when(fixture.babies().findByIdAndOwnerUserId(BABY, OWNER))
                .thenReturn(java.util.Optional.empty());
        when(fixture.familyAuthorization().hasPermission(
                GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        when(fixture.familyAuthorization().hasPermission(
                GROUP, FAMILY, PermissionFlag.CHECKLIST_COMPLETE)).thenReturn(true);

        assertThat(fixture.policy().canView(reminder, FAMILY)).isFalse();
        assertThat(fixture.policy().canComplete(reminder, FAMILY)).isFalse();
        assertThat(fixture.policy().presentationGroup(reminder, FAMILY)).isEmpty();
    }

    private static Fixture fixture() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy familyAuthorization = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        return new Fixture(
                new ReminderAccessPolicy(groups, familyAuthorization, journeys, babies),
                groups, familyAuthorization, journeys, babies);
    }

    private static CareGroup activeGroup(UUID id) {
        return CareGroup.builder()
                .id(id)
                .ownerUserId(OWNER)
                .linkedJourneyId(JOURNEY)
                .status(CareGroupStatus.ACTIVE)
                .build();
    }

    private record Fixture(
            ReminderAccessPolicy policy,
            CareGroupRepository groups,
            CareGroupAuthorizationPolicy familyAuthorization,
            MotherJourneyRepository journeys,
            BabyProfileRepository babies) {
    }
}
