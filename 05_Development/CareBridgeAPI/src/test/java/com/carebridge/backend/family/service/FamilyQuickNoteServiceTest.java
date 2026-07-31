package com.carebridge.backend.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.repository.MaternalHealthMetricRepository;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
    @Mock private MaternalHealthMetricRepository metricRepository;

    private final UUID groupId = UUID.randomUUID();
    private final UUID familyId = UUID.randomUUID();
    private final UUID motherId = UUID.randomUUID();
    private final UUID journeyId = UUID.randomUUID();
    private final Instant from = Instant.parse("2026-07-31T00:00:00Z");
    private final Instant to = Instant.parse("2026-07-31T23:59:59Z");
    private FamilyQuickNoteService service;

    @BeforeEach
    void setUp() {
        service = new FamilyQuickNoteService(
                groupRepository, authorizationPolicy, journeyRepository, metricRepository);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(CareGroup.builder()
                .id(groupId)
                .ownerUserId(motherId)
                .groupName("Gia đình")
                .build()));
        when(authorizationPolicy.isMember(groupId, familyId)).thenReturn(true);
        when(authorizationPolicy.isOwner(groupId, familyId)).thenReturn(false);
    }

    @Test
    void allowedWeightHistoryIsReadOnlyAndNewestFirst() {
        allow(PermissionFlag.QUICK_NOTE_WEIGHT);
        when(journeyRepository.findCanonical(motherId)).thenReturn(Optional.of(
                MotherJourney.builder().id(journeyId).ownerUserId(motherId).build()));
        var older = metric(MetricType.WEIGHT, "61.2", from.plusSeconds(60), "private note");
        var newer = metric(MetricType.WEIGHT, "62.1", from.plusSeconds(120), "private note");
        when(metricRepository
                .findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        journeyId, MetricType.WEIGHT, MetricStatus.ACTIVE, from, to))
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
                MotherJourney.builder().id(journeyId).ownerUserId(motherId).build()));
        when(metricRepository
                .findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        journeyId, MetricType.EPDS_SCORE, MetricStatus.ACTIVE, from, to))
                .thenReturn(List.of(metric(
                        MetricType.EPDS_SCORE,
                        "12",
                        from.plusSeconds(60),
                        "{\"answers\":[3,2,1]}")));

        var response = service.getHistory(groupId, familyId, MetricType.EPDS_SCORE, from, to);

        assertThat(response.getDataPoints()).singleElement()
                .satisfies(point -> assertThat(point.getNote()).isNull());
    }

    @Test
    void fetalMovementEventTypeRemainsVisible() {
        allow(PermissionFlag.QUICK_NOTE_FETAL_MOVEMENT);
        when(journeyRepository.findCanonical(motherId)).thenReturn(Optional.of(
                MotherJourney.builder().id(journeyId).ownerUserId(motherId).build()));
        when(metricRepository
                .findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        journeyId, MetricType.FETAL_MOVEMENT_COUNT, MetricStatus.ACTIVE, from, to))
                .thenReturn(List.of(metric(
                        MetricType.FETAL_MOVEMENT_COUNT, "1", from.plusSeconds(60), "KICK")));

        var response = service.getHistory(
                groupId, familyId, MetricType.FETAL_MOVEMENT_COUNT, from, to);

        assertThat(response.getDataPoints()).singleElement()
                .satisfies(point -> assertThat(point.getNote()).isEqualTo("KICK"));
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
        verify(metricRepository, never())
                .findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc(
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

    private MaternalHealthMetric metric(
            MetricType type, String value, Instant measuredAt, String note) {
        return MaternalHealthMetric.builder()
                .id(UUID.randomUUID())
                .journeyId(journeyId)
                .metricType(type)
                .valueNumeric(new BigDecimal(value))
                .unit(type == MetricType.WEIGHT ? "kg" : "")
                .measuredAt(measuredAt)
                .sourceType(DataSource.MANUAL)
                .note(note)
                .build();
    }
}
