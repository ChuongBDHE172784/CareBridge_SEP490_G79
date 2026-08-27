package com.carebridge.backend.baby.policy;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BabyAccessPolicyTest {

    @Mock
    private CareGroupRepository careGroupRepository;

    @Mock
    private CareGroupAuthorizationPolicy authorizationPolicy;

    @InjectMocks
    private BabyAccessPolicy policy;

    @Test
    void ownerCanViewAndManageWithoutDelegatedPermission() {
        UUID ownerId = UUID.randomUUID();
        BabyProfile profile = profile(ownerId);

        assertThat(policy.canView(profile, ownerId)).isTrue();
        assertThat(policy.canManageJournal(profile, ownerId)).isTrue();
        assertThat(policy.canManageGrowth(profile, ownerId)).isTrue();
    }

    @Test
    void acceptedMemberNeedsExplicitPermissionForEachWriteDomain() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        BabyProfile profile = profile(ownerId);
        CareGroup group = CareGroup.builder().id(groupId).linkedBabyProfileId(profile.getId()).status(CareGroupStatus.ACTIVE).build();
        when(careGroupRepository.findByLinkedBabyProfileId(profile.getId())).thenReturn(List.of(group));
        when(authorizationPolicy.isMember(groupId, memberId)).thenReturn(true);
        when(authorizationPolicy.hasPermission(groupId, memberId, PermissionFlag.BABY_VIEW)).thenReturn(true);
        when(authorizationPolicy.hasPermission(groupId, memberId, PermissionFlag.BABY_JOURNAL_WRITE)).thenReturn(true);
        when(authorizationPolicy.hasPermission(groupId, memberId, PermissionFlag.BABY_GROWTH_WRITE)).thenReturn(false);

        assertThat(policy.canView(profile, memberId)).isTrue();
        assertThat(policy.canManageJournal(profile, memberId)).isTrue();
        assertThat(policy.canManageGrowth(profile, memberId)).isFalse();
    }

    @Test
    void pendingOrUnrelatedMemberIsDenied() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        BabyProfile profile = profile(ownerId);
        CareGroup group = CareGroup.builder().id(groupId).linkedBabyProfileId(profile.getId()).status(CareGroupStatus.ACTIVE).build();
        when(careGroupRepository.findByLinkedBabyProfileId(profile.getId())).thenReturn(List.of(group));
        when(authorizationPolicy.isMember(groupId, memberId)).thenReturn(false);

        assertThat(policy.canView(profile, memberId)).isFalse();
        assertThat(policy.canManageJournal(profile, memberId)).isFalse();
    }

    @Test
    void archivedGroupIsDeniedEvenWithAcceptedPermission() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        BabyProfile profile = profile(ownerId);
        CareGroup group = CareGroup.builder().id(groupId).linkedBabyProfileId(profile.getId())
                .status(CareGroupStatus.ARCHIVED).build();
        when(careGroupRepository.findByLinkedBabyProfileId(profile.getId())).thenReturn(List.of(group));
        assertThat(policy.canView(profile, memberId)).isFalse();
    }

    private BabyProfile profile(UUID ownerId) {
        return BabyProfile.builder().id(UUID.randomUUID()).ownerUserId(ownerId).nickname("Test baby").build();
    }
}
