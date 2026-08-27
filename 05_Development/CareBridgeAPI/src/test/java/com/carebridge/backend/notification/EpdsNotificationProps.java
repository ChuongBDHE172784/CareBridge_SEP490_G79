package com.carebridge.backend.notification;

import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.health.event.EpdsScreeningCompleted;
import java.time.Instant;
import java.util.UUID;
import static org.mockito.Mockito.lenient;

/**
 * Props Isolation factory for EpdsFamilyNotification tests (CB-EPDS-TEST-001 §4.1).
 *
 * <p>All test data is constructed here — tests must not inline entity construction.
 * Identifiers are fixed and synthetic; no real PII.
 */
public final class EpdsNotificationProps {

    private EpdsNotificationProps() {
    }

    public static final Instant FIXED_NOW = Instant.parse("2026-08-14T09:00:00Z");
    public static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    public static final UUID GROUP_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000011");
    public static final UUID FAMILY_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    public static final UUID FAMILY_2 = UUID.fromString("00000000-0000-0000-0000-000000000102");
    public static final UUID OBSERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000900");
    public static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000800");

    public static EpdsScreeningCompleted makeEvent(int totalScore, int question10Score) {
        return new EpdsScreeningCompleted(
                OBSERVATION_ID, JOURNEY_ID, MOTHER_ID, totalScore, question10Score, FIXED_NOW);
    }

    public static CareGroupMember makeFamilyMember(UUID userId) {
        return makeFamilyMember(userId, GROUP_ID);
    }

    public static CareGroupMember makeFamilyMember(UUID userId, UUID careGroupId) {
        return CareGroupMember.builder()
                .careGroupId(careGroupId)
                .userId(userId)
                .inviteStatus(InviteStatus.ACCEPTED)
                .build();
    }

    /**
     * Stubs the two-flag consent gate for one (group, user) pair.
     *
     * @param parent whether QUICK_NOTES (parent flag) is granted
     * @param child  whether QUICK_NOTE_EPDS (child flag) is granted
     */
    public static void allowEpds(CareGroupAuthorizationPolicy policy, UUID groupId, UUID userId,
                                 boolean parent, boolean child) {
        lenient().when(policy.hasPermission(groupId, userId, PermissionFlag.QUICK_NOTES))
                .thenReturn(parent);
        lenient().when(policy.hasPermission(groupId, userId, PermissionFlag.QUICK_NOTE_EPDS))
                .thenReturn(child);
    }
}
