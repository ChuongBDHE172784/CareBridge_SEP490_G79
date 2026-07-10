package com.carebridge.backend.carejourney.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.GrowthChartTestFactory;
import com.carebridge.backend.carejourney.dto.AddGrowthMeasurementRequest;
import com.carebridge.backend.carejourney.dto.GrowthChartResponse;
import com.carebridge.backend.carejourney.dto.GrowthMeasurementHistoryItem;
import com.carebridge.backend.carejourney.dto.GrowthMeasurementResponse;
import com.carebridge.backend.carejourney.dto.UpdateGrowthMeasurementRequest;
import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import com.carebridge.backend.carejourney.repository.GrowthMeasurementRepository;
import com.carebridge.backend.carejourney.service.impl.GrowthServiceImpl;
import com.carebridge.backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.carebridge.backend.carejourney.GrowthChartTestFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrowthServiceTest {

    @Mock private BabyProfileRepository babyProfileRepository;
    @Mock private GrowthMeasurementRepository growthMeasurementRepository;
    @Mock private AuditService auditService;
    @InjectMocks private GrowthServiceImpl growthService;

    // GROWTH-TC-038-001: Happy path with measurements
    @Test
    void getGrowthChart_happyPath_returnsMeasurementsSortedAscWithAgeInDays() {
        BabyProfile baby = makeBaby();
        List<GrowthMeasurement> measurements = makeMeasurements();

        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(BABY_ID))
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
        when(growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(BABY_ID))
                .thenReturn(List.of());

        GrowthChartResponse response = growthService.getGrowthChart(MOTHER_ID, BABY_ID);

        assertThat(response).isNotNull();
        assertThat(response.getMeasurements()).isNotNull().isEmpty();
    }

    // GROWTH-TC-038-003: ARCHIVED baby -> 200 (allowed per ADR-BABY-008-002)
    @Test
    void getGrowthChart_archivedBaby_returns200() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeArchivedBaby()));
        when(growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(BABY_ID))
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

    @Test
    void addGrowthMeasurement_validRequest_savesMeasurement() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBaby()));
        when(growthMeasurementRepository.save(any(GrowthMeasurement.class)))
                .thenAnswer(invocation -> {
                    GrowthMeasurement saved = invocation.getArgument(0);
                    saved.setGrowthMeasurementId(UUID.fromString("aaaa0003-0000-0000-0000-000000000003"));
                    return saved;
                });

        AddGrowthMeasurementRequest request = new AddGrowthMeasurementRequest();
        request.setMeasuredDate(LocalDate.of(2026, 4, 1));
        request.setWeightKg(new BigDecimal("6.20"));

        GrowthMeasurementResponse response = growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request);

        assertThat(response.getBabyId()).isEqualTo(BABY_ID);
        assertThat(response.getWeightKg()).isEqualByComparingTo("6.20");
        verify(growthMeasurementRepository).save(any(GrowthMeasurement.class));
    }

    @Test
    void addGrowthMeasurement_noValues_throwsBaby072() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBaby()));

        AddGrowthMeasurementRequest request = new AddGrowthMeasurementRequest();
        request.setMeasuredDate(LocalDate.of(2026, 4, 1));

        assertThatThrownBy(() -> growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("BABY-072"));
        verify(growthMeasurementRepository, never()).save(any());
    }

    @Test
    void addGrowthMeasurement_archivedBaby_throwsBaby073() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeArchivedBaby()));

        AddGrowthMeasurementRequest request = new AddGrowthMeasurementRequest();
        request.setMeasuredDate(LocalDate.of(2026, 4, 1));
        request.setHeightCm(new BigDecimal("60"));

        assertThatThrownBy(() -> growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("BABY-073");
                });
        verify(growthMeasurementRepository, never()).save(any());
    }

    @Test
    void addGrowthMeasurement_negativeValue_throwsBaby074() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBaby()));

        AddGrowthMeasurementRequest request = new AddGrowthMeasurementRequest();
        request.setMeasuredDate(LocalDate.of(2026, 4, 1));
        request.setWeightKg(new BigDecimal("-1.00"));

        assertThatThrownBy(() -> growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getCode()).isEqualTo("BABY-074");
                });
        verify(growthMeasurementRepository, never()).save(any());
    }

    @Test
    void updateGrowthMeasurement_validPartialUpdate_savesAndAudits() {
        GrowthMeasurement measurement = makeMeasurements().get(0);
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBaby()));
        when(growthMeasurementRepository.findById(measurement.getGrowthMeasurementId()))
                .thenReturn(Optional.of(measurement));
        when(growthMeasurementRepository.save(measurement)).thenReturn(measurement);

        UpdateGrowthMeasurementRequest request = new UpdateGrowthMeasurementRequest();
        request.setHeightCm(new BigDecimal("61.5"));
        request.setNote("updated height");

        GrowthMeasurementResponse response = growthService.updateGrowthMeasurement(
                MOTHER_ID, BABY_ID, measurement.getGrowthMeasurementId(), request);

        assertThat(response.getHeightCm()).isEqualByComparingTo("61.5");
        assertThat(response.getNote()).isEqualTo("updated height");
        verify(growthMeasurementRepository).save(measurement);
        verify(auditService).log(eq(com.carebridge.backend.audit.entity.AuditAction.GROWTH_MEASUREMENT_UPDATED),
                eq(MOTHER_ID), eq("GROWTH_MEASUREMENT"), eq(measurement.getGrowthMeasurementId().toString()), anyString());
    }

    @Test
    void updateGrowthMeasurement_emptyPayload_throwsBaby076BeforeLookup() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBaby()));

        UpdateGrowthMeasurementRequest request = new UpdateGrowthMeasurementRequest();

        assertThatThrownBy(() -> growthService.updateGrowthMeasurement(
                MOTHER_ID, BABY_ID, UUID.randomUUID(), request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("BABY-076"));
        verify(growthMeasurementRepository, never()).findById(any());
    }

    @Test
    void updateGrowthMeasurement_wrongBaby_throwsBaby079AndDoesNotSave() {
        GrowthMeasurement measurement = makeMeasurements().get(0);
        UUID otherBabyId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000039");
        measurement.setBabyId(otherBabyId);
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBaby()));
        when(growthMeasurementRepository.findById(measurement.getGrowthMeasurementId()))
                .thenReturn(Optional.of(measurement));

        UpdateGrowthMeasurementRequest request = new UpdateGrowthMeasurementRequest();
        request.setWeightKg(new BigDecimal("6.00"));

        assertThatThrownBy(() -> growthService.updateGrowthMeasurement(
                MOTHER_ID, BABY_ID, measurement.getGrowthMeasurementId(), request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("BABY-079");
                });
        verify(growthMeasurementRepository, never()).save(any());
    }

    @Test
    void deleteGrowthMeasurement_existingMeasurement_setsDeletedAt() {
        GrowthMeasurement measurement = makeMeasurements().get(0);
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBaby()));
        when(growthMeasurementRepository.findById(measurement.getGrowthMeasurementId()))
                .thenReturn(Optional.of(measurement));

        growthService.deleteGrowthMeasurement(MOTHER_ID, BABY_ID, measurement.getGrowthMeasurementId());

        assertThat(measurement.getDeletedAt()).isNotNull();
        verify(growthMeasurementRepository).save(measurement);
    }

    @Test
    void deleteGrowthMeasurement_wrongBaby_throwsBaby081() {
        GrowthMeasurement measurement = makeMeasurements().get(0);
        measurement.setBabyId(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000039"));
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBaby()));
        when(growthMeasurementRepository.findById(measurement.getGrowthMeasurementId()))
                .thenReturn(Optional.of(measurement));

        assertThatThrownBy(() -> growthService.deleteGrowthMeasurement(
                MOTHER_ID, BABY_ID, measurement.getGrowthMeasurementId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("BABY-081");
                });
        verify(growthMeasurementRepository, never()).save(any());
    }

    @Test
    void deleteGrowthMeasurement_alreadyDeleted_returnsWithoutSaving() {
        GrowthMeasurement measurement = makeMeasurements().get(0);
        measurement.setDeletedAt(Instant.parse("2026-04-01T00:00:00Z"));
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBaby()));
        when(growthMeasurementRepository.findById(measurement.getGrowthMeasurementId()))
                .thenReturn(Optional.of(measurement));

        growthService.deleteGrowthMeasurement(MOTHER_ID, BABY_ID, measurement.getGrowthMeasurementId());

        verify(growthMeasurementRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void getGrowthMeasurementHistory_validRequest_returnsPage() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBaby()));
        PageRequest pageable = PageRequest.of(0, 20);
        when(growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(BABY_ID, pageable))
                .thenReturn(new PageImpl<>(makeMeasurements(), pageable, 2));

        Page<GrowthMeasurementHistoryItem> response =
                growthService.getGrowthMeasurementHistory(MOTHER_ID, BABY_ID, pageable);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getGrowthMeasurementId())
                .isEqualTo(makeMeasurements().get(0).getGrowthMeasurementId());
    }

    @Test
    void getGrowthMeasurementHistory_archivedBaby_isAllowed() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeArchivedBaby()));
        PageRequest pageable = PageRequest.of(0, 20);
        when(growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(BABY_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<GrowthMeasurementHistoryItem> response =
                growthService.getGrowthMeasurementHistory(MOTHER_ID, BABY_ID, pageable);

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    void getGrowthMeasurementHistory_notOwner_throwsBaby071BeforeQuery() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBabyOwnedByOther()));

        assertThatThrownBy(() -> growthService.getGrowthMeasurementHistory(
                MOTHER_ID, BABY_ID, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("BABY-071");
                });
        verify(growthMeasurementRepository, never())
                .findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(any(), any());
    }

    @Test
    void getGrowthMeasurementHistory_invalidPageSize_throwsBaby084BeforeBabyLookup() {
        assertThatThrownBy(() -> growthService.getGrowthMeasurementHistory(
                MOTHER_ID, BABY_ID, PageRequest.of(0, 51)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getCode()).isEqualTo("BABY-084");
                });
        verifyNoInteractions(babyProfileRepository);
    }
}
