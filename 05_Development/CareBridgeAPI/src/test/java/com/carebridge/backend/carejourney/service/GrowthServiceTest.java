package com.carebridge.backend.carejourney.service;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.GrowthChartTestFactory;
import com.carebridge.backend.carejourney.dto.GrowthChartResponse;
import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import com.carebridge.backend.carejourney.repository.GrowthMeasurementRepository;
import com.carebridge.backend.carejourney.service.impl.GrowthServiceImpl;
import com.carebridge.backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static com.carebridge.backend.carejourney.GrowthChartTestFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrowthServiceTest {

    @Mock private BabyProfileRepository babyProfileRepository;
    @Mock private GrowthMeasurementRepository growthMeasurementRepository;
    @InjectMocks private GrowthServiceImpl growthService;

    // GROWTH-TC-038-001: Happy path with measurements
    @Test
    void getGrowthChart_happyPath_returnsMeasurementsSortedAscWithAgeInDays() {
        BabyProfile baby = makeBaby();
        List<GrowthMeasurement> measurements = makeMeasurements();

        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(growthMeasurementRepository.findByBabyIdOrderByMeasuredDateAsc(BABY_ID))
                .thenReturn(measurements);

        GrowthChartResponse response = growthService.getGrowthChart(MOTHER_ID, BABY_ID);

        assertThat(response).isNotNull();
        assertThat(response.getBabyId()).isEqualTo(BABY_ID);
        assertThat(response.getNickname()).isEqualTo("Growth Baby");
        assertThat(response.getBirthDate()).isEqualTo(baby.getBirthDate());
        assertThat(response.getMeasurements()).hasSize(2);
        assertThat(response.getMeasurements().get(0).getMeasuredDate())
                .isEqualTo(measurements.get(0).getMeasuredDate());
        assertThat(response.getMeasurements().get(0).getAgeInDays()).isEqualTo(31);
        assertThat(response.getMeasurements().get(1).getAgeInDays()).isEqualTo(59);
    }

    // GROWTH-TC-038-002: No measurements -> 200 with empty list
    @Test
    void getGrowthChart_noMeasurements_returnsEmptyList() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBaby()));
        when(growthMeasurementRepository.findByBabyIdOrderByMeasuredDateAsc(BABY_ID))
                .thenReturn(List.of());

        GrowthChartResponse response = growthService.getGrowthChart(MOTHER_ID, BABY_ID);

        assertThat(response).isNotNull();
        assertThat(response.getMeasurements()).isNotNull().isEmpty();
    }

    // GROWTH-TC-038-003: ARCHIVED baby -> 200 (allowed per ADR-BABY-008-002)
    @Test
    void getGrowthChart_archivedBaby_returns200() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeArchivedBaby()));
        when(growthMeasurementRepository.findByBabyIdOrderByMeasuredDateAsc(BABY_ID))
                .thenReturn(makeMeasurements());

        assertThatNoException().isThrownBy(() ->
                growthService.getGrowthChart(MOTHER_ID, BABY_ID));
    }

    // GROWTH-TC-038-004: Baby not owned -> 403 BABY-071
    @Test
    void getGrowthChart_babyNotOwned_throwsForbidden() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBabyOwnedByOther()));

        assertThatThrownBy(() -> growthService.getGrowthChart(MOTHER_ID, BABY_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("BABY-071");
                });
    }

    // GROWTH-TC-038-005: Baby not found -> 404 BABY-070
    @Test
    void getGrowthChart_babyNotFound_throwsNotFound() {
        when(babyProfileRepository.findById(NON_EXISTENT_BABY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> growthService.getGrowthChart(MOTHER_ID, NON_EXISTENT_BABY_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("BABY-070");
                });
    }
}
