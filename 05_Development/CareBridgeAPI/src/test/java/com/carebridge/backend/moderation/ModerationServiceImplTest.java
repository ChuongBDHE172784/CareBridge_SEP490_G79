package com.carebridge.backend.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.dto.request.ModerationHistoryFilter;
import com.carebridge.backend.content.dto.request.ModerationQueueFilter;
import com.carebridge.backend.content.dto.request.PendingContentQueueFilter;
import com.carebridge.backend.content.dto.response.ModerationHistoryResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.dto.response.PendingContentQueueResponse;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.mapper.ModerationMapper;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import com.carebridge.backend.content.service.ContentPreviewService;
import com.carebridge.backend.content.service.ModerationServiceImpl;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

    @Mock
    private CommunityQuestionRepository communityQuestionRepository;

    @Mock
    private CommunityAnswerRepository communityAnswerRepository;

    @Mock
    private ModerationActionRepository moderationActionRepository;

    @Mock
    private UserRepository userRepository;

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

    // PCQ-TC-001: targetType=QUESTION returns CommunityQuestion PENDING items, only that repo queried
    @Test
    void getPendingContentQueue_withQuestionTargetType_returnsQuestionItemsOnly() {
        CommunityQuestion q1 = PendingContentTestFactory.pendingQuestion();
        CommunityQuestion q2 = PendingContentTestFactory.pendingQuestion();
        Page<CommunityQuestion> page = new PageImpl<>(List.of(q1, q2));

        when(communityQuestionRepository.findByStatus(eq(QuestionStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);
        when(contentPreviewService.batchFetchPreviews(any(), eq(ReportTargetType.QUESTION)))
                .thenReturn(Map.of(q1.getId(), q1.getBody(), q2.getId(), q2.getBody()));

        PendingContentQueueFilter filter = new PendingContentQueueFilter(ReportTargetType.QUESTION, 0, 20);
        PendingContentQueueResponse response = moderationService.getPendingContentQueue(filter, mockPrincipal);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).targetType()).isEqualTo(ReportTargetType.QUESTION);
        assertThat(response.totalElements()).isEqualTo(2);
        verify(communityQuestionRepository, times(1)).findByStatus(eq(QuestionStatus.PENDING), any(Pageable.class));
        verify(communityAnswerRepository, times(0)).findByStatus(any(), any());
    }

    // PCQ-TC-002: targetType=ANSWER returns CommunityAnswer PENDING items, only that repo queried
    @Test
    void getPendingContentQueue_withAnswerTargetType_returnsAnswerItemsOnly() {
        CommunityAnswer a1 = PendingContentTestFactory.pendingAnswer();
        Page<CommunityAnswer> page = new PageImpl<>(List.of(a1));

        when(communityAnswerRepository.findByStatus(eq(AnswerStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);
        when(contentPreviewService.batchFetchPreviews(any(), eq(ReportTargetType.ANSWER)))
                .thenReturn(Map.of(a1.getId(), a1.getBody()));

        PendingContentQueueFilter filter = new PendingContentQueueFilter(ReportTargetType.ANSWER, 0, 20);
        PendingContentQueueResponse response = moderationService.getPendingContentQueue(filter, mockPrincipal);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).targetType()).isEqualTo(ReportTargetType.ANSWER);
        verify(communityAnswerRepository, times(1)).findByStatus(eq(AnswerStatus.PENDING), any(Pageable.class));
        verify(communityQuestionRepository, times(0)).findByStatus(any(), any());
    }

    // PCQ-TC-003: targetType=CONTENT is rejected with MOD-023
    @Test
    void getPendingContentQueue_withContentTargetType_throwsUnsupported() {
        PendingContentQueueFilter filter = new PendingContentQueueFilter(ReportTargetType.CONTENT, 0, 20);

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.getPendingContentQueue(filter, mockPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-023");
        verify(communityQuestionRepository, times(0)).findByStatus(any(), any());
        verify(communityAnswerRepository, times(0)).findByStatus(any(), any());
    }

    // PCQ-TC-004: targetType=ACCOUNT is rejected with MOD-023
    @Test
    void getPendingContentQueue_withAccountTargetType_throwsUnsupported() {
        PendingContentQueueFilter filter = new PendingContentQueueFilter(ReportTargetType.ACCOUNT, 0, 20);

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.getPendingContentQueue(filter, mockPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-023");
    }

    // PCQ-TC-006: contentPreview comes from ContentPreviewService (no authorId ever leaves the DTO —
    // enforced by PendingContentItemResponse's record shape, verified here at the data level)
    @Test
    void getPendingContentQueue_previewComesFromContentPreviewServiceOnly() {
        CommunityQuestion q1 = PendingContentTestFactory.pendingQuestion();
        Page<CommunityQuestion> page = new PageImpl<>(List.of(q1));

        when(communityQuestionRepository.findByStatus(eq(QuestionStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);
        when(contentPreviewService.batchFetchPreviews(any(), eq(ReportTargetType.QUESTION)))
                .thenReturn(Map.of(q1.getId(), "truncated preview only"));

        PendingContentQueueFilter filter = new PendingContentQueueFilter(ReportTargetType.QUESTION, 0, 20);
        PendingContentQueueResponse response = moderationService.getPendingContentQueue(filter, mockPrincipal);

        assertThat(response.content().get(0).contentPreview()).isEqualTo("truncated preview only");
        assertThat(response.content().get(0).targetId()).isEqualTo(q1.getId());
    }

    // PCQ-TC-009: no PENDING content → empty content list, no exception
    @Test
    void getPendingContentQueue_withNoPendingContent_returnsEmptyList() {
        when(communityQuestionRepository.findByStatus(eq(QuestionStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PendingContentQueueFilter filter = new PendingContentQueueFilter(ReportTargetType.QUESTION, 0, 20);
        PendingContentQueueResponse response = moderationService.getPendingContentQueue(filter, mockPrincipal);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
    }

    private ModerationAction makeAction(UUID actionId, UUID targetId, ReportTargetType targetType,
            ModerationActionType actionType, UUID moderatorUserId, String reason) {
        return ModerationAction.builder()
                .id(actionId)
                .targetId(targetId)
                .targetType(targetType)
                .actionType(actionType)
                .moderatorUserId(moderatorUserId)
                .reason(reason)
                .actionAt(Instant.now())
                .build();
    }

    // PCQH-TC-001: targetType=null returns both QUESTION and ANSWER actions
    @Test
    void getModerationHistory_withNullTargetType_returnsBothTypes() {
        UUID modId = UUID.randomUUID();
        ModerationAction q = makeAction(UUID.randomUUID(), TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, modId, null);
        ModerationAction a = makeAction(UUID.randomUUID(), TARGET_ID_2, ReportTargetType.ANSWER,
                ModerationActionType.HIDE, modId, "spam");
        when(moderationActionRepository.findByTargetTypeInOrderByActionAtDesc(
                eq(List.of(ReportTargetType.QUESTION, ReportTargetType.ANSWER)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(q, a)));
        when(userRepository.findAllById(any())).thenReturn(
                List.of(User.builder().id(modId).name("Moderator Test").build()));
        when(contentPreviewService.batchFetchPreviews(any(), any())).thenReturn(Map.of());

        ModerationHistoryFilter filter = new ModerationHistoryFilter(null, 0, 20);
        ModerationHistoryResponse response = moderationService.getModerationHistory(filter, mockPrincipal);

        assertThat(response.content()).hasSize(2);
    }

    // PCQH-TC-002: targetType=QUESTION filters correctly
    @Test
    void getModerationHistory_withQuestionTargetType_returnsOnlyQuestion() {
        UUID modId = UUID.randomUUID();
        ModerationAction q = makeAction(UUID.randomUUID(), TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, modId, null);
        when(moderationActionRepository.findByTargetTypeInOrderByActionAtDesc(
                eq(List.of(ReportTargetType.QUESTION)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(q)));
        when(userRepository.findAllById(any())).thenReturn(
                List.of(User.builder().id(modId).name("Moderator Test").build()));
        when(contentPreviewService.batchFetchPreviews(any(), any())).thenReturn(Map.of());

        ModerationHistoryFilter filter = new ModerationHistoryFilter(ReportTargetType.QUESTION, 0, 20);
        ModerationHistoryResponse response = moderationService.getModerationHistory(filter, mockPrincipal);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).targetType()).isEqualTo(ReportTargetType.QUESTION);
    }

    // PCQH-TC-003: moderatorName resolved via batch UserRepository.findAllById()
    @Test
    void getModerationHistory_resolvesModeratorNameViaBatch() {
        UUID modId = UUID.randomUUID();
        ModerationAction q = makeAction(UUID.randomUUID(), TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, modId, null);
        when(moderationActionRepository.findByTargetTypeInOrderByActionAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(q)));
        when(userRepository.findAllById(any())).thenReturn(
                List.of(User.builder().id(modId).name("Moderator Test").build()));
        when(contentPreviewService.batchFetchPreviews(any(), any())).thenReturn(Map.of());

        ModerationHistoryFilter filter = new ModerationHistoryFilter(null, 0, 20);
        ModerationHistoryResponse response = moderationService.getModerationHistory(filter, mockPrincipal);

        assertThat(response.content().get(0).moderatorName()).isEqualTo("Moderator Test");
    }

    // PCQH-TC-004: reason is returned verbatim, not truncated
    @Test
    void getModerationHistory_returnsReasonVerbatim() {
        UUID modId = UUID.randomUUID();
        ModerationAction a = makeAction(UUID.randomUUID(), TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.HIDE, modId, "Nội dung không phù hợp");
        when(moderationActionRepository.findByTargetTypeInOrderByActionAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a)));
        when(userRepository.findAllById(any())).thenReturn(
                List.of(User.builder().id(modId).name("Moderator Test").build()));
        when(contentPreviewService.batchFetchPreviews(any(), any())).thenReturn(Map.of());

        ModerationHistoryFilter filter = new ModerationHistoryFilter(null, 0, 20);
        ModerationHistoryResponse response = moderationService.getModerationHistory(filter, mockPrincipal);

        assertThat(response.content().get(0).reason()).isEqualTo("Nội dung không phù hợp");
    }
}
