package com.carebridge.backend.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.repository.HealthObservationRepository;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class FamilyQuickNoteServiceTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;
    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private HealthObservationRepository observationRepository;

    private final UUID groupId = UUID.randomUUID();
    private final UUID familyId = UUID.randomUUID();
    private final UUID motherId = UUID.randomUUID();
    private final UUID journeyId = UUID.randomUUID();
    private final UUID careSubjectId = UUID.randomUUID();
    private final Instant from = Instant.parse("2026-07-31T00:00:00Z");
    private final Instant to = Instant.parse("2026-07-31T23:59:59Z");
    private FamilyQuickNoteService service;

    @BeforeEach
    void setUp() {
        service = new FamilyQuickNoteService(
                groupRepository, authorizationPolicy, journeyRepository, observationRepository);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(CareGroup.builder()
                .id(groupId)
                .ownerUserId(motherId)
                .groupName("Gia đình")
                .status(CareGroupStatus.ACTIVE)
                .build()));
        lenient().when(authorizationPolicy.isMember(groupId, familyId)).thenReturn(true);
        lenient().when(authorizationPolicy.isOwner(groupId, familyId)).thenReturn(false);
    }

    @Test
    void allowedWeightHistoryIsReadOnlyAndNewestFirst() {
        allow(PermissionFlag.QUICK_NOTE_WEIGHT);
        when(journeyRepository.findCanonical(motherId)).thenReturn(Optional.of(
                MotherJourney.builder().id(journeyId).ownerUserId(motherId)
                        .careSubjectId(careSubjectId).build()));
        var older = observation("WEIGHT", "61.2", from.plusSeconds(60), "private note", Map.of());
        var newer = observation("WEIGHT", "62.1", from.plusSeconds(120), "private note", Map.of());
        when(observationRepository.findTrend(
                        careSubjectId, "WEIGHT", MetricStatus.ACTIVE, from, to))
                .thenReturn(List.of(older, newer));

        var response = service.getHistory(groupId, familyId, MetricType.WEIGHT, from, to);

        assertThat(response.getDataPoints()).extracting(point -> point.getValueNumeric())
                .containsExactly(new BigDecimal("62.1"), new BigDecimal("61.2"));
        assertThat(response.getDataPoints()).allSatisfy(point -> assertThat(point.getNote()).isNull());
    }

    @Test
    void epdsAnswerPayloadIsNotExposedToFamilyHistory() {
        allow(PermissionFlag.QUICK_NOTE_EPDS);
        when(journeyRepository.findCanonical(motherId)).thenReturn(Optional.of(
                MotherJourney.builder().id(journeyId).ownerUserId(motherId)
                        .careSubjectId(careSubjectId).build()));
        when(observationRepository.findTrend(
                        careSubjectId, "EPDS_SCORE", MetricStatus.ACTIVE, from, to))
                .thenReturn(List.of(HealthObservation.builder()
                        .id(UUID.randomUUID()).careSubjectId(careSubjectId)
                        .metricCode("EPDS_SCORE")
                        .valueNumeric(new BigDecimal("12"))
                        .valueSecondary(new BigDecimal("3"))
                        .measuredAt(from.plusSeconds(60))
                        .sourceType(DataSource.MANUAL)
                        .note("{\"answers\":[3,2,1]}")
                        .context(Map.of("answers", List.of(3, 2, 1), "question10", 3))
                        .build()));

        var response = service.getHistory(groupId, familyId, MetricType.EPDS_SCORE, from, to);

        assertThat(response.getDataPoints()).singleElement()
                .satisfies(point -> {
                    assertThat(point.getNote()).isNull();
                    assertThat(point.getContext()).isEmpty();
                    assertThat(point.getValueSecondary()).isNull();
                });
    }

    @Test
    void unsupportedGlucoseContextValueIsNotExposed() {
        allow(PermissionFlag.QUICK_NOTE_BLOOD_GLUCOSE);
        when(journeyRepository.findCanonical(motherId)).thenReturn(Optional.of(
                MotherJourney.builder().id(journeyId).ownerUserId(motherId)
                        .careSubjectId(careSubjectId).build()));
        when(observationRepository.findTrend(
                        careSubjectId, "BLOOD_GLUCOSE", MetricStatus.ACTIVE, from, to))
                .thenReturn(List.of(observation(
                        "BLOOD_GLUCOSE", "96", from.plusSeconds(60), "private note",
                        Map.of("measurementContext", "private note"))));

        var response = service.getHistory(
                groupId, familyId, MetricType.BLOOD_GLUCOSE, from, to);

        assertThat(response.getDataPoints()).singleElement()
                .satisfies(point -> assertThat(point.getContext()).isEmpty());
    }

    @Test
    void archivedGroupCannotReadHealthHistory() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(CareGroup.builder()
                .id(groupId).ownerUserId(motherId).groupName("Gia đình")
                .status(CareGroupStatus.ARCHIVED).build()));

        assertThatThrownBy(() -> service.getHistory(
                groupId, familyId, MetricType.WEIGHT, from, to))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        verify(observationRepository, never()).findTrend(any(), any(), any(), any(), any());
    }

    @Test
    void fetalMovementHistoryExposesOnlySafeSessionContext() {
        allow(PermissionFlag.QUICK_NOTE_FETAL_MOVEMENT);
        when(journeyRepository.findCanonical(motherId)).thenReturn(Optional.of(
                MotherJourney.builder().id(journeyId).ownerUserId(motherId)
                        .careSubjectId(careSubjectId).build()));
        when(observationRepository.findTrend(
                        careSubjectId, "FETAL_MOVEMENT_SESSION", MetricStatus.ACTIVE, from, to))
                .thenReturn(List.of(observation(
                        "FETAL_MOVEMENT_SESSION", "7", from.plusSeconds(60), "private note",
                        Map.of("protocolCode", "COUNT_TO_TEN", "completionStatus", "COMPLETED",
                                "gestationalAgeSnapshot", "28w", "privateField", "secret"))));

        var response = service.getHistory(
                groupId, familyId, MetricType.FETAL_MOVEMENT_COUNT, from, to);

        assertThat(response.getDataPoints()).singleElement()
                .satisfies(point -> {
                    assertThat(point.getNote()).isNull();
                    assertThat(point.getContext()).containsOnlyKeys(
                            "protocolCode", "completionStatus", "gestationalAgeSnapshot");
                });
    }

    @Test
    void bloodPressureAndGlucoseUseCanonicalObservationsAndSanitizeContext() {
        allow(PermissionFlag.QUICK_NOTE_BLOOD_PRESSURE);
        allow(PermissionFlag.QUICK_NOTE_BLOOD_GLUCOSE);
        when(journeyRepository.findCanonical(motherId)).thenReturn(Optional.of(
                MotherJourney.builder().id(journeyId).ownerUserId(motherId)
                        .careSubjectId(careSubjectId).build()));
        when(observationRepository.findTrend(
                        careSubjectId, "BLOOD_PRESSURE", MetricStatus.ACTIVE, from, to))
                .thenReturn(List.of(HealthObservation.builder()
                        .id(UUID.randomUUID()).careSubjectId(careSubjectId)
                        .metricCode("BLOOD_PRESSURE")
                        .valueNumeric(new BigDecimal("118"))
                        .valueSecondary(new BigDecimal("76"))
                        .unit("mmHg").measuredAt(from.plusSeconds(60))
                        .context(Map.of("privateField", "secret")).build()));
        when(observationRepository.findTrend(
                        careSubjectId, "BLOOD_GLUCOSE", MetricStatus.ACTIVE, from, to))
                .thenReturn(List.of(observation(
                        "BLOOD_GLUCOSE", "96", from.plusSeconds(120), "private note",
                        Map.of("measurementContext", "FASTING", "privateField", "secret"))));

        var pressure = service.getHistory(groupId, familyId, MetricType.BLOOD_PRESSURE, from, to);
        var glucose = service.getHistory(groupId, familyId, MetricType.BLOOD_GLUCOSE, from, to);

        assertThat(pressure.getDataPoints()).singleElement().satisfies(point -> {
            assertThat(point.getValueNumeric()).isEqualByComparingTo("118");
            assertThat(point.getValueSecondary()).isEqualByComparingTo("76");
            assertThat(point.getContext()).isEmpty();
        });
        assertThat(glucose.getDataPoints()).singleElement().satisfies(point ->
                assertThat(point.getContext()).containsOnly(Map.entry("measurementContext", "FASTING")));
    }

    @Test
    void parentPermissionOffDeniesEvenWhenChildIsOn() {
        when(authorizationPolicy.hasPermission(groupId, familyId, PermissionFlag.QUICK_NOTES))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getHistory(
                groupId, familyId, MetricType.HYDRATION, from, to))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        verify(observationRepository, never())
                .findTrend(
                        any(), any(), any(), any(), any());
    }

    @Test
    void childPermissionOffDeniesRequestedHistory() {
        when(authorizationPolicy.hasPermission(groupId, familyId, PermissionFlag.QUICK_NOTES))
                .thenReturn(true);
        when(authorizationPolicy.hasPermission(
                groupId, familyId, PermissionFlag.QUICK_NOTE_HYDRATION)).thenReturn(false);

        assertThatThrownBy(() -> service.getHistory(
                groupId, familyId, MetricType.HYDRATION, from, to))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("FAM-067"));
    }

    private void allow(PermissionFlag child) {
        when(authorizationPolicy.hasPermission(groupId, familyId, PermissionFlag.QUICK_NOTES))
                .thenReturn(true);
        when(authorizationPolicy.hasPermission(groupId, familyId, child)).thenReturn(true);
    }

    private HealthObservation observation(
            String metricCode, String value, Instant measuredAt, String note, Map<String, Object> context) {
        return HealthObservation.builder()
                .id(UUID.randomUUID())
                .careSubjectId(careSubjectId)
                .metricCode(metricCode)
                .valueNumeric(new BigDecimal(value))
                .unit("WEIGHT".equals(metricCode) ? "kg" : "")
                .measuredAt(measuredAt)
                .sourceType(DataSource.MANUAL)
                .note(note)
                .context(context)
                .build();
    }
}
