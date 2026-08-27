package com.carebridge.backend.baby;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;

import java.time.Instant;
import java.util.UUID;

/** Props-isolation factory for UC33 ArchiveBabyProfile unit tests. */
public final class BabyProfileArchiveTestFactory {

    public static final UUID MOTHER_ID        = UUID.fromString("00000000-0000-0000-0000-000000000033");
    public static final UUID BABY_ID          = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000033");
    public static final UUID OTHER_MOTHER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final UUID OTHER_BABY_ID    = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000099");
    public static final UUID ARCHIVED_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000088");
    public static final UUID UNKNOWN_BABY_ID  = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private BabyProfileArchiveTestFactory() {}

    public static BabyProfile makeActiveBaby() {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(MOTHER_ID)
                .nickname("Archive Bean")
                .status(BabyProfileStatus.ACTIVE)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    public static BabyProfile makeArchivedBaby() {
        return BabyProfile.builder()
                .id(ARCHIVED_BABY_ID)
                .ownerUserId(MOTHER_ID)
                .nickname("Already Archived Bean")
                .status(BabyProfileStatus.ARCHIVED)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    public static BabyProfile makeOtherMotherBaby() {
        return BabyProfile.builder()
                .id(OTHER_BABY_ID)
                .ownerUserId(OTHER_MOTHER_ID)
                .nickname("Other Mother Bean")
                .status(BabyProfileStatus.ACTIVE)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
