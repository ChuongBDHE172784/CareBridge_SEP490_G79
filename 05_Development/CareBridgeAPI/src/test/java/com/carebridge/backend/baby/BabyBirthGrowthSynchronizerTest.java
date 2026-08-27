package com.carebridge.backend.baby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.service.BabyBirthGrowthSynchronizer;
import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import com.carebridge.backend.carejourney.repository.GrowthMeasurementStore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BabyBirthGrowthSynchronizerTest {

    private static final UUID BABY_ID = UUID.fromString("00000000-0000-0000-0000-000000007401");
    private static final LocalDate BIRTH_DATE = LocalDate.of(2026, 8, 1);

    @Mock private GrowthMeasurementStore growthMeasurementStore;

    private BabyBirthGrowthSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        synchronizer = new BabyBirthGrowthSynchronizer(growthMeasurementStore);
    }

    @Test
    void knownBirthMeasurementsProjectToOneDeterministicSession() {
        synchronizer.synchronize(profile(new BigDecimal("3.25"), new BigDecimal("50.0")));

        ArgumentCaptor<GrowthMeasurement> captor = ArgumentCaptor.forClass(GrowthMeasurement.class);
        verify(growthMeasurementStore).save(captor.capture());
        GrowthMeasurement saved = captor.getValue();
        assertThat(saved.getGrowthMeasurementId())
                .isEqualTo(BabyBirthGrowthSynchronizer.deterministicSessionId(BABY_ID));
        assertThat(saved.getBabyId()).isEqualTo(BABY_ID);
        assertThat(saved.getCareSubjectId()).isEqualTo(BABY_ID);
        assertThat(saved.getMeasuredDate()).isEqualTo(BIRTH_DATE);
        assertThat(saved.getWeightKg()).isEqualByComparingTo("3.25");
        assertThat(saved.getHeightCm()).isEqualByComparingTo("50.0");
        assertThat(saved.getSourceType()).isEqualTo(BabyBirthGrowthSynchronizer.BIRTH_RECORD_SOURCE);
    }

    @Test
    void partialBirthMeasurementWritesOnlyTheSuppliedValue() {
        synchronizer.synchronize(profile(null, new BigDecimal("49.5")));

        ArgumentCaptor<GrowthMeasurement> captor = ArgumentCaptor.forClass(GrowthMeasurement.class);
        verify(growthMeasurementStore).save(captor.capture());
        assertThat(captor.getValue().getWeightKg()).isNull();
        assertThat(captor.getValue().getHeightCm()).isEqualByComparingTo("49.5");
    }

    @Test
    void absentBirthMeasurementsDoNotCreateAnEmptyGrowthSession() {
        synchronizer.synchronize(profile(null, null));

        verify(growthMeasurementStore, never()).save(any());
    }

    @Test
    void repeatedProjectionUsesTheSameSessionIdentity() {
        BabyProfile profile = profile(new BigDecimal("3.10"), new BigDecimal("48.0"));
        synchronizer.synchronize(profile);
        synchronizer.synchronize(profile);

        ArgumentCaptor<GrowthMeasurement> captor = ArgumentCaptor.forClass(GrowthMeasurement.class);
        verify(growthMeasurementStore, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(GrowthMeasurement::getGrowthMeasurementId)
                .containsOnly(BabyBirthGrowthSynchronizer.deterministicSessionId(BABY_ID));
    }

    @Test
    void invalidContextFailsWithoutLeakingMeasurements() {
        BabyProfile missingId = profile(new BigDecimal("3.10"), null);
        missingId.setId(null);

        assertThatThrownBy(() -> synchronizer.synchronize(missingId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Baby birth Growth context is unavailable");
        verify(growthMeasurementStore, never()).save(any());
    }

    @Test
    void storeFailureIsPropagatedWithoutAddingSensitiveValuesToTheError() {
        doThrow(new IllegalStateException("Growth projection unavailable"))
                .when(growthMeasurementStore).save(any());

        assertThatThrownBy(() -> synchronizer.synchronize(
                profile(new BigDecimal("3.10"), new BigDecimal("48.0"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Growth projection unavailable")
                .hasMessageNotContaining("3.10")
                .hasMessageNotContaining("48.0");
    }

    private BabyProfile profile(BigDecimal weight, BigDecimal length) {
        return BabyProfile.builder()
                .id(BABY_ID)
                .birthDate(BIRTH_DATE)
                .birthWeightKg(weight)
                .birthLengthCm(length)
                .build();
    }
}
