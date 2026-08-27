package com.carebridge.backend.baby.service;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import com.carebridge.backend.carejourney.repository.GrowthMeasurementStore;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Projects birth measurements into the canonical baby Growth history. */
@Component
@RequiredArgsConstructor
public class BabyBirthGrowthSynchronizer {

    public static final String BIRTH_RECORD_SOURCE = "BIRTH_RECORD";
    private static final String SESSION_ID_PREFIX = "baby-birth-growth:";

    private final GrowthMeasurementStore growthMeasurementStore;

    /**
     * Creates or repairs the deterministic birth session. No row is needed when the profile
     * contains neither optional birth measurement.
     */
    public void synchronize(BabyProfile profile) {
        if (profile == null
                || (profile.getBirthWeightKg() == null && profile.getBirthLengthCm() == null)) {
            return;
        }
        if (profile.getId() == null || profile.getBirthDate() == null) {
            throw new IllegalStateException("Baby birth Growth context is unavailable");
        }

        growthMeasurementStore.save(GrowthMeasurement.builder()
                .growthMeasurementId(deterministicSessionId(profile.getId()))
                .babyId(profile.getId())
                .careSubjectId(profile.getId())
                .measuredDate(profile.getBirthDate())
                .weightKg(profile.getBirthWeightKg())
                .heightCm(profile.getBirthLengthCm())
                .sourceType(BIRTH_RECORD_SOURCE)
                .build());
    }

    public static UUID deterministicSessionId(UUID babyId) {
        if (babyId == null) {
            throw new IllegalArgumentException("Baby identity is required");
        }
        return UUID.nameUUIDFromBytes(
                (SESSION_ID_PREFIX + babyId).getBytes(StandardCharsets.UTF_8));
    }
}
