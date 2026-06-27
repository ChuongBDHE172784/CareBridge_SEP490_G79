package com.carebridge.backend.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.dto.request.ModerationQueueFilter;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.mapper.ModerationMapper;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.service.ContentPreviewService;
import com.carebridge.backend.content.service.ModerationServiceImpl;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

// MOD-TC-001, MOD-TC-002, MOD-TC-004, MOD-TC-005
@ExtendWith(MockitoExtension.class)
class ModerationServiceImplTest {

    @Mock
    private ContentReportRepository contentReportRepository;

    @Mock
    private ContentPreviewService contentPreviewService;

    @Mock
    private AuditService auditService;

    @Spy
    private ModerationMapper moderationMapper = new ModerationMapper();

    @InjectMocks
    private ModerationServiceImpl moderationService;

    private static final UUID REPORT_ID_1 = UUID.fromString("11111111-0000-0000-0000-000000000001");
    private static final UUID REPORT_ID_2 = UUID.fromString("11111111-0000-0000-0000-000000000002");
    private static final UUID TARGET_ID_1 = UUID.fromString("22222222-0000-0000-0000-000000000001");
    private static final UUID TARGET_ID_2 = UUID.fromString("22222222-0000-0000-0000-000000000002");

    private Principal mockPrincipal;

    @BeforeEach
    void setUp() {
        mockPrincipal = () -> "1";
    }

    private ContentReport makeReport(UUID id, UUID targetId, ReportTargetType type, ReportStatus status) {
        ContentReport r = new ContentReport();
        r.setId(id);
        r.setTargetId(targetId);
        r.setTargetType(type);
        r.setStatus(status);
        r.setCategory("Inappropriate content");
        r.setCreatedAt(Instant.now());
        return r;
    }

    // MOD-TC-001: filter QUESTION/PENDING → only QUESTION items returned, AuditService called
    @Test
    void getModerationQueue_withQuestionFilter_returnsOnlyQuestionItems() {
        // BR-MOD-001, BR-MOD-002: filter by contentType and status
        ContentReport report = makeReport(REPORT_ID_1, TARGET_ID_1, ReportTargetType.QUESTION, ReportStatus.PENDING);
        Page<ContentReport> page = new PageImpl<>(List.of(report));

        when(contentReportRepository.findByStatusAndTargetType(
                eq(ReportStatus.PENDING), eq(ReportTargetType.QUESTION), any(Pageable.class)))
                .thenReturn(page);
        when(contentReportRepository.countByTargetIdAndStatus(eq(TARGET_ID_1), eq(ReportStatus.PENDING))).thenReturn(1L);
        when(contentPreviewService.fetchPreview(eq(TARGET_ID_1), eq(ReportTargetType.QUESTION))).thenReturn("Preview text");

        ModerationQueueFilter filter = new ModerationQueueFilter(
                ReportTargetType.QUESTION, ReportStatus.PENDING, 0, 20);

        ModerationQueueResponse response = moderationService.getModerationQueue(filter, mockPrincipal);

        // Assert results
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).targetType()).isEqualTo(ReportTargetType.QUESTION);
        assertThat(response.content().get(0).status()).isEqualTo(ReportStatus.PENDING);
        assertThat(response.totalElements()).isEqualTo(1);

        // Verify repository called with contentType filter
        verify(contentReportRepository, times(1))
                .findByStatusAndTargetType(eq(ReportStatus.PENDING), eq(ReportTargetType.QUESTION), any(Pageable.class));

        // C2: AuditService must be called after each queue view (ADR-003)
        verify(auditService, times(1)).log(
                eq(AuditAction.MODERATION_QUEUE_VIEWED), eq("1"), isNull(), any());
    }

    // MOD-TC-002: no targetType filter → findByStatus (all types) called
    @Test
    void getModerationQueue_withNoTargetType_queriesAllTypes() {
        // BR-MOD-001: when no contentType, query all PENDING
        ContentReport q = makeReport(REPORT_ID_1, TARGET_ID_1, ReportTargetType.QUESTION, ReportStatus.PENDING);
        ContentReport a = makeReport(REPORT_ID_2, TARGET_ID_2, ReportTargetType.ANSWER, ReportStatus.PENDING);
        Page<ContentReport> page = new PageImpl<>(List.of(q, a));

        when(contentReportRepository.findByStatus(eq(ReportStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);
        when(contentReportRepository.countByTargetIdAndStatus(any(UUID.class), any())).thenReturn(1L);
        when(contentPreviewService.fetchPreview(any(UUID.class), any())).thenReturn("Preview");

        ModerationQueueFilter filter = new ModerationQueueFilter(null, ReportStatus.PENDING, 0, 20);

        ModerationQueueResponse response = moderationService.getModerationQueue(filter, mockPrincipal);

        assertThat(response.content()).hasSize(2);
        // findByStatus must be called, NOT findByStatusAndTargetType
        verify(contentReportRepository, times(1)).findByStatus(eq(ReportStatus.PENDING), any(Pageable.class));

        verify(auditService, times(1)).log(
                eq(AuditAction.MODERATION_QUEUE_VIEWED), eq("1"), isNull(), any());
    }

    // MOD-TC-004: AuditService called 3 times when getModerationQueue called 3 times
    @Test
    void getModerationQueue_calledMultipleTimes_auditLoggedEachTime() {
        // ADR-003, BR-AUDIT-001
        Page<ContentReport> emptyPage = new PageImpl<>(List.of());
        when(contentReportRepository.findByStatus(any(), any(Pageable.class))).thenReturn(emptyPage);

        ModerationQueueFilter filter = new ModerationQueueFilter(null, ReportStatus.PENDING, 0, 20);

        moderationService.getModerationQueue(filter, mockPrincipal);
        moderationService.getModerationQueue(filter, mockPrincipal);
        moderationService.getModerationQueue(filter, mockPrincipal);

        // C2: audit called exactly 3 times
        verify(auditService, times(3)).log(
                eq(AuditAction.MODERATION_QUEUE_VIEWED), eq("1"), isNull(), any());
    }

    // MOD-TC-005: Pageable has Sort by createdAt DESC
    @Test
    void getModerationQueue_pageable_hasCorrectSortOrder() {
        // BR-MOD-003: C5 constraint — sort by reportedAt DESC (createdAt in DB)
        Page<ContentReport> emptyPage = new PageImpl<>(List.of());
        ArgumentCaptor<Pageable> pageableCaptor = forClass(Pageable.class);

        when(contentReportRepository.findByStatus(any(), pageableCaptor.capture())).thenReturn(emptyPage);

        ModerationQueueFilter filter = new ModerationQueueFilter(null, ReportStatus.PENDING, 0, 20);
        moderationService.getModerationQueue(filter, mockPrincipal);

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getSort().getOrderFor("createdAt"))
                .isNotNull()
                .satisfies(order -> assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC));
    }
}
