package com.carebridge.backend.baby;

import com.carebridge.backend.baby.dto.UpdateBabyProfileRequest;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.entity.Gender;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Props-isolation factory for UC32 UpdateBabyProfile unit tests. */
public final class BabyProfileUpdateTestFactory {

    public static final UUID MOTHER_ID        = UUID.fromString("00000000-0000-0000-0000-000000000032");
    public static final UUID BABY_ID          = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000032");
    public static final UUID OTHER_MOTHER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final UUID OTHER_BABY_ID    = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000099");
    public static final UUID ARCHIVED_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000088");
    public static final UUID UNKNOWN_BABY_ID  = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private BabyProfileUpdateTestFactory() {}

    public static BabyProfile makeActiveBaby() {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(MOTHER_ID)
                .nickname("Bean")
                .birthDate(LocalDate.of(2026, 1, 1))
                .gender(Gender.FEMALE)
                .birthWeightKg(new BigDecimal("3.2"))
                .birthLengthCm(new BigDecimal("50.0"))
                .status(BabyProfileStatus.ACTIVE)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    public static BabyProfile makeArchivedBaby() {
        return BabyProfile.builder()
                .id(ARCHIVED_BABY_ID)
                .ownerUserId(MOTHER_ID)
                .nickname("Archived Bean")
                .status(BabyProfileStatus.ARCHIVED)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    public static BabyProfile makeOtherMotherBaby() {
        return BabyProfile.builder()
                .id(OTHER_BABY_ID)
                .ownerUserId(OTHER_MOTHER_ID)
                .nickname("Other Bean")
                .status(BabyProfileStatus.ACTIVE)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    public static UpdateBabyProfileRequest makeUpdateRequest() {
        UpdateBabyProfileRequest req = new UpdateBabyProfileRequest();
        req.setNickname("Updated Bean");
        req.setBirthDate(LocalDate.of(2026, 2, 10));
        req.setGender(Gender.MALE);
        req.setBirthWeightKg(new BigDecimal("3.5"));
        req.setBirthLengthCm(new BigDecimal("52.0"));
        return req;
    }
}
