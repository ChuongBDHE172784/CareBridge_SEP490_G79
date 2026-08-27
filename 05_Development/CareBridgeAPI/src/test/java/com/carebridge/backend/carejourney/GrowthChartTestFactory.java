package com.carebridge.backend.carejourney;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.carejourney.entity.GrowthMeasurement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class GrowthChartTestFactory {

    public static final UUID MOTHER_ID          = UUID.fromString("00000000-0000-0000-0000-000000000038");
    public static final UUID OTHER_USER_ID      = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final UUID BABY_ID            = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000038");
    public static final UUID NON_EXISTENT_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-999999999999");

    public static BabyProfile makeBaby() {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(MOTHER_ID)
                .nickname("Growth Baby")
                .birthDate(LocalDate.of(2026, 1, 15))
                .status(BabyProfileStatus.ACTIVE)
                .build();
    }

    public static BabyProfile makeArchivedBaby() {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(MOTHER_ID)
                .nickname("Growth Baby")
                .birthDate(LocalDate.of(2026, 1, 15))
                .status(BabyProfileStatus.ARCHIVED)
                .build();
    }

    public static BabyProfile makeBabyOwnedByOther() {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(OTHER_USER_ID)
                .nickname("Growth Baby")
                .birthDate(LocalDate.of(2026, 1, 15))
                .status(BabyProfileStatus.ACTIVE)
                .build();
    }

    public static BabyProfile makeBabyWithNullBirthDate() {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(MOTHER_ID)
                .nickname("Growth Baby")
                .birthDate(null)
                .status(BabyProfileStatus.ACTIVE)
                .build();
    }

    public static List<GrowthMeasurement> makeMeasurements() {
        return List.of(
                GrowthMeasurement.builder()
                        .growthMeasurementId(UUID.fromString("aaaa0001-0000-0000-0000-000000000001"))
                        .babyId(BABY_ID)
                        .measuredDate(LocalDate.of(2026, 2, 15))
                        .weightKg(new BigDecimal("4.2"))
                        .heightCm(new BigDecimal("52"))
                        .headCircumferenceCm(new BigDecimal("35"))
                        .note("1 month checkup")
                        .build(),
                GrowthMeasurement.builder()
                        .growthMeasurementId(UUID.fromString("aaaa0002-0000-0000-0000-000000000002"))
                        .babyId(BABY_ID)
                        .measuredDate(LocalDate.of(2026, 3, 15))
                        .weightKg(new BigDecimal("5.1"))
                        .heightCm(new BigDecimal("55"))
                        .headCircumferenceCm(new BigDecimal("37"))
                        .note("2 month checkup")
                        .build()
        );
    }

    public static List<GrowthMeasurement> makeMeasurements(Consumer<List<GrowthMeasurement>> overrides) {
        List<GrowthMeasurement> list = new ArrayList<>(makeMeasurements());
        overrides.accept(list);
        return list;
    }
}
