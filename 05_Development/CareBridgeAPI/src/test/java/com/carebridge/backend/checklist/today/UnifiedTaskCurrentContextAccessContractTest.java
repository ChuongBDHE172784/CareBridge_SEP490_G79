package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskAccessPolicy;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UnifiedTaskCurrentContextAccessContractTest {

    private static final UUID MOTHER = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID FAMILY = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID GROUP = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID OLD_BABY = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID CURRENT_BABY = UUID.fromString("00000000-0000-0000-0000-000000000302");

    @Test
    void motherIsDeniedWhenInstanceContextIsNoLongerLinkedToItsCareGroup() {
        var fixture = fixture();

        assertThat(fixture.policy().canView(instance(MOTHER, ChecklistRecipientRole.MOTHER), MOTHER)).isFalse();
        assertThat(fixture.policy().canComplete(instance(MOTHER, ChecklistRecipientRole.MOTHER), MOTHER)).isFalse();
    }

    @Test
    void familyIsDeniedWhenOwnerMatchesButInstanceContextIsNoLongerLinked() {
        var fixture = fixture();
        when(fixture.familyPolicy().hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        when(fixture.familyPolicy().hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_COMPLETE)).thenReturn(true);

        assertThat(fixture.policy().canView(instance(FAMILY, ChecklistRecipientRole.FAMILY), FAMILY)).isFalse();
        assertThat(fixture.policy().canComplete(instance(FAMILY, ChecklistRecipientRole.FAMILY), FAMILY)).isFalse();
    }

    @Test
    void familyCurrentlyLinkedActiveCanonicalContextDoesNotRequireReviewedMirror() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy familyPolicy = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        when(groups.findById(GROUP)).thenReturn(Optional.of(CareGroup.builder()
                .id(GROUP)
                .ownerUserId(MOTHER)
                .linkedBabyProfileId(CURRENT_BABY)
                .status(CareGroupStatus.ACTIVE)
                .build()));
        when(babies.findByIdAndOwnerUserId(CURRENT_BABY, MOTHER)).thenReturn(Optional.of(
                BabyProfile.builder().id(CURRENT_BABY).ownerUserId(MOTHER)
                        .status(BabyProfileStatus.ACTIVE).build()));
        when(familyPolicy.hasPermission(GROUP, FAMILY, PermissionFlag.CHECKLIST_VIEW)).thenReturn(true);
        UnifiedTaskAccessPolicy policy = new UnifiedTaskAccessPolicy(
                groups, familyPolicy, journeys, babies);

        assertThat(policy.canView(instance(FAMILY, ChecklistRecipientRole.FAMILY, CURRENT_BABY), FAMILY))
                .isTrue();
    }

    private static Fixture fixture() {
        CareGroupRepository groups = mock(CareGroupRepository.class);
        CareGroupAuthorizationPolicy familyPolicy = mock(CareGroupAuthorizationPolicy.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        when(groups.findById(GROUP)).thenReturn(Optional.of(CareGroup.builder()
                .id(GROUP)
                .ownerUserId(MOTHER)
                .linkedBabyProfileId(CURRENT_BABY)
                .status(CareGroupStatus.ACTIVE)
                .build()));
        when(babies.findByIdAndOwnerUserId(CURRENT_BABY, MOTHER)).thenReturn(Optional.of(
                BabyProfile.builder().id(CURRENT_BABY).ownerUserId(MOTHER)
                        .status(BabyProfileStatus.ACTIVE).build()));
        return new Fixture(
                new UnifiedTaskAccessPolicy(groups, familyPolicy, journeys, babies), familyPolicy);
    }

    private static ChecklistInstance instance(UUID recipient, ChecklistRecipientRole role) {
        return instance(recipient, role, OLD_BABY);
    }

    private static ChecklistInstance instance(UUID recipient, ChecklistRecipientRole role, UUID contextId) {
        return ChecklistInstance.builder()
                .recipientUserId(recipient)
                .recipientRole(role)
                .careGroupId(GROUP)
                .careContextType(ChecklistCareContextType.BABY)
                .careContextId(contextId)
                .contextOwnerUserId(MOTHER)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .build();
    }

    private record Fixture(UnifiedTaskAccessPolicy policy, CareGroupAuthorizationPolicy familyPolicy) {
    }
}
