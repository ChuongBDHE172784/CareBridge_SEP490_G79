package com.carebridge.backend.health.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IStorageService;
import com.carebridge.backend.health.HealthRecord42TestFactory;
import com.carebridge.backend.health.dto.TimelineFilter;
import com.carebridge.backend.health.dto.TimelineResponse;
import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.repository.HealthRecordFileRepository;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import com.carebridge.backend.health.service.impl.HealthRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static com.carebridge.backend.health.HealthRecord42TestFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTimelineTest {

    @Mock private HealthRecordRepository recordRepository;
    @Mock private HealthRecordFileRepository recordFileRepository;
    @Mock private UploadedFileRepository uploadedFileRepository;
    @Mock private IStorageService storageService;
    @Mock private AuditService auditService;
    @InjectMocks private HealthRecordServiceImpl service;

    private TimelineFilter defaultFilter() {
        TimelineFilter f = new TimelineFilter();
        f.setPage(0);
        f.setSize(20);
        return f;
    }

    private Page<HealthRecord> pageOf(List<HealthRecord> records, TimelineFilter filter) {
        return new PageImpl<>(records, PageRequest.of(filter.getPage(), filter.getSize()), records.size());
    }

    // HEALTH42-TC-001: Happy path — returns ACTIVE records sorted
    @Test
    void getTimeline_happyPath_returnsActiveRecords() {
        List<HealthRecord> records = List.of(makeActiveLabResult(), makeActiveUltrasound());
        TimelineFilter filter = defaultFilter();

        when(recordRepository.findActiveByOwnerFiltered(eq(ACC_001), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(pageOf(records, filter));

        TimelineResponse response = service.getTimeline(ACC_001, filter);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
    }

    // HEALTH42-TC-002: ARCHIVED records excluded
    @Test
    void getTimeline_archivedRecordsExcluded_onlyActiveReturned() {
        List<HealthRecord> activeOnly = List.of(makeActiveLabResult());
        TimelineFilter filter = defaultFilter();

        when(recordRepository.findActiveByOwnerFiltered(eq(ACC_001), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(pageOf(activeOnly, filter));

        TimelineResponse response = service.getTimeline(ACC_001, filter);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getHealthRecordId()).isEqualTo(HR_A);
    }

    // HEALTH42-TC-003: Other user's records excluded
    @Test
    void getTimeline_otherUserRecordsExcluded_onlyOwnerRecordsReturned() {
        List<HealthRecord> ownerOnly = List.of(makeActiveLabResult());
        TimelineFilter filter = defaultFilter();

        when(recordRepository.findActiveByOwnerFiltered(eq(ACC_001), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(pageOf(ownerOnly, filter));

        TimelineResponse response = service.getTimeline(ACC_001, filter);

        assertThat(response.getItems()).allSatisfy(item ->
                assertThat(item.getHealthRecordId()).isNotEqualTo(HR_D));
    }

    // HEALTH42-TC-004: Filter by record type
    @Test
    void getTimeline_filterByRecordType_onlyMatchingTypeReturned() {
        List<HealthRecord> labResults = List.of(makeActiveLabResult());
        TimelineFilter filter = defaultFilter();
        filter.setRecordType("LAB_RESULT");

        when(recordRepository.findActiveByOwnerFiltered(eq(ACC_001), eq("LAB_RESULT"), isNull(), isNull(), isNull(), any()))
                .thenReturn(pageOf(labResults, filter));

        TimelineResponse response = service.getTimeline(ACC_001, filter);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getRecordType()).isEqualTo("LAB_RESULT");
    }

    // HEALTH42-TC-005: Filter by journey
    @Test
    void getTimeline_filterByJourney_onlyMatchingJourneyReturned() {
        List<HealthRecord> journeyRecords = List.of(makeActiveLabResult(), makeActiveUltrasound());
        TimelineFilter filter = defaultFilter();
        filter.setJourneyId(JOURNEY_1);

        when(recordRepository.findActiveByOwnerFiltered(eq(ACC_001), isNull(), eq(JOURNEY_1), isNull(), isNull(), any()))
                .thenReturn(pageOf(journeyRecords, filter));

        TimelineResponse response = service.getTimeline(ACC_001, filter);

        assertThat(response.getItems()).hasSize(2);
    }

    // HEALTH42-TC-006: Filter by baby
    @Test
    void getTimeline_filterByBaby_onlyMatchingBabyReturned() {
        List<HealthRecord> babyRecords = List.of(makeActivePrescriptionForBaby());
        TimelineFilter filter = defaultFilter();
        filter.setBabyId(BABY_001);

        when(recordRepository.findActiveByOwnerFiltered(eq(ACC_001), isNull(), isNull(), eq(BABY_001), isNull(), any()))
                .thenReturn(pageOf(babyRecords, filter));

        TimelineResponse response = service.getTimeline(ACC_001, filter);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getBabyId()).isEqualTo(BABY_001);
    }

    // HEALTH42-TC-007: Empty result -> 200 with empty items list
    @Test
    void getTimeline_noRecords_returnsEmptyList() {
        TimelineFilter filter = defaultFilter();

        when(recordRepository.findActiveByOwnerFiltered(eq(ACC_001), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(Page.empty());

        TimelineResponse response = service.getTimeline(ACC_001, filter);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotNull().isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    // HEALTH42-TC-008: Pagination parameters passed correctly
    @Test
    void getTimeline_pagination_correctPageableConstructed() {
        TimelineFilter filter = new TimelineFilter();
        filter.setPage(2);
        filter.setSize(10);

        when(recordRepository.findActiveByOwnerFiltered(eq(ACC_001), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(Page.empty(PageRequest.of(2, 10)));

        TimelineResponse response = service.getTimeline(ACC_001, filter);

        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(10);
    }
}
