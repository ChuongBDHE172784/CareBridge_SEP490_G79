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
import com.carebridge.backend.content.dto.request.RevertReportRequest;
import com.carebridge.backend.content.dto.response.ModerationContentDetailResponse;
import com.carebridge.backend.content.dto.response.AccountViolationHistoryResponse;
import com.carebridge.backend.content.dto.response.AccountViolationSummaryResponse;
import com.carebridge.backend.content.dto.response.ModerationHistoryResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.dto.response.PendingContentQueueResponse;
import com.carebridge.backend.content.dto.response.RelatedReportPageResponse;
import com.carebridge.backend.content.dto.response.RevertReportResponse;
import com.carebridge.backend.content.dto.response.UndoModerationActionResponse;
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
        when(moderationActionRepository.findByTargetTypeInAndActionTypeNotOrderByActionAtDesc(
                eq(List.of(ReportTargetType.QUESTION, ReportTargetType.ANSWER)), eq(ModerationActionType.AI_FEEDBACK_SUBMITTED), any(Pageable.class)))
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
        when(moderationActionRepository.findByTargetTypeInAndActionTypeNotOrderByActionAtDesc(
                eq(List.of(ReportTargetType.QUESTION)), eq(ModerationActionType.AI_FEEDBACK_SUBMITTED), any(Pageable.class)))
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
        when(moderationActionRepository.findByTargetTypeInAndActionTypeNotOrderByActionAtDesc(any(), eq(ModerationActionType.AI_FEEDBACK_SUBMITTED), any(Pageable.class)))
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
        when(moderationActionRepository.findByTargetTypeInAndActionTypeNotOrderByActionAtDesc(any(), eq(ModerationActionType.AI_FEEDBACK_SUBMITTED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a)));
        when(userRepository.findAllById(any())).thenReturn(
                List.of(User.builder().id(modId).name("Moderator Test").build()));
        when(contentPreviewService.batchFetchPreviews(any(), any())).thenReturn(Map.of());

        ModerationHistoryFilter filter = new ModerationHistoryFilter(null, 0, 20);
        ModerationHistoryResponse response = moderationService.getModerationHistory(filter, mockPrincipal);

        assertThat(response.content().get(0).reason()).isEqualTo("Nội dung không phù hợp");
    }

    @Test
    void getAccountViolationHistory_groupsActionsByAccountWithSafeNameFallbacks() {
        UUID missingTargetId = UUID.randomUUID();
        UUID moderatorId = UUID.randomUUID();
        ModerationAction latest = makeAction(UUID.randomUUID(), missingTargetId, ReportTargetType.ACCOUNT,
                ModerationActionType.SUSPEND, moderatorId, "Vi phạm quy tắc");
        latest.setExpiresAt(Instant.now().plusSeconds(3600));
        ModerationAction earlier = makeAction(UUID.randomUUID(), missingTargetId, ReportTargetType.ACCOUNT,
                ModerationActionType.WARN, moderatorId, "Đã cảnh cáo trước đó");
        Page<UUID> targetPage = new PageImpl<>(List.of(missingTargetId),
                org.springframework.data.domain.PageRequest.of(0, 20), 1);
        ArgumentCaptor<Pageable> pageableCaptor = forClass(Pageable.class);
        when(moderationActionRepository.findDistinctAccountTargetIds(any(), pageableCaptor.capture()))
                .thenReturn(targetPage);
        when(moderationActionRepository.findAccountActionsByTargetIds(any(), any()))
                .thenReturn(List.of(latest, earlier));
        when(userRepository.findAllById(any())).thenReturn(List.of(User.builder()
                .id(moderatorId).name("Moderator Test").build()));

        AccountViolationSummaryResponse response = moderationService.getAccountViolationHistory(0, 20, mockPrincipal);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).targetUserName()).isEqualTo("Tài khoản không còn tồn tại");
        assertThat(response.content().get(0).violationCount()).isEqualTo(2);
        assertThat(response.content().get(0).latestAction().moderatorName()).isEqualTo("Moderator Test");
        assertThat(response.content().get(0).latestAction().expiresAt()).isEqualTo(latest.getExpiresAt());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void getAccountViolationHistory_withNoAccounts_returnsEmptyPage() {
        when(moderationActionRepository.findDistinctAccountTargetIds(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AccountViolationSummaryResponse response = moderationService.getAccountViolationHistory(0, 20, mockPrincipal);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void getAccountViolationHistory_detailReturnsAllActionsForOneAccountNewestFirst() {
        UUID targetUserId = UUID.randomUUID();
        UUID moderatorId = UUID.randomUUID();
        ModerationAction latest = makeAction(UUID.randomUUID(), targetUserId, ReportTargetType.ACCOUNT,
                ModerationActionType.RESTRICT, moderatorId, "Lặp lại hành vi vi phạm");
        Page<ModerationAction> actionPage = new PageImpl<>(List.of(latest),
                org.springframework.data.domain.PageRequest.of(0, 20), 1);
        when(moderationActionRepository.findAccountActionsByTargetId(eq(targetUserId), any(), any(Pageable.class)))
                .thenReturn(actionPage);
        when(userRepository.findAllById(any())).thenReturn(List.of(
                User.builder().id(targetUserId).name("Tài khoản Test").build(),
                User.builder().id(moderatorId).name("Moderator Test").build()));

        AccountViolationHistoryResponse response = moderationService.getAccountViolationHistory(
                targetUserId, 0, 20, mockPrincipal);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).reason()).isEqualTo("Lặp lại hành vi vi phạm");
        assertThat(response.content().get(0).targetUserName()).isEqualTo("Tài khoản Test");
    }

    @Test
    void getRelatedReports_returnsOnlyReportsForTheSelectedTargetNewestFirst() {
        ContentReport selected = makeReport(REPORT_ID_1, TARGET_ID_1, ReportTargetType.QUESTION, ReportStatus.PENDING);
        ContentReport related = makeReport(REPORT_ID_2, TARGET_ID_1, ReportTargetType.QUESTION, ReportStatus.RESOLVED);
        related.setCategory("SPAM");
        related.setCreatedAt(Instant.now().plusSeconds(60));
        ArgumentCaptor<Pageable> pageableCaptor = forClass(Pageable.class);
        when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.of(selected));
        when(contentReportRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(
                eq(TARGET_ID_1), eq(ReportTargetType.QUESTION), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(related), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        RelatedReportPageResponse response = moderationService.getRelatedReports(REPORT_ID_1, 0, 20, mockPrincipal);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(REPORT_ID_2);
        assertThat(response.content().get(0).category()).isEqualTo("SPAM");
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getRelatedReports_withNoMatchingReports_returnsEmptyPage() {
        ContentReport selected = makeReport(REPORT_ID_1, TARGET_ID_1, ReportTargetType.QUESTION, ReportStatus.PENDING);
        when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.of(selected));
        when(contentReportRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(
                eq(TARGET_ID_1), eq(ReportTargetType.QUESTION), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        RelatedReportPageResponse response = moderationService.getRelatedReports(REPORT_ID_1, 0, 20, mockPrincipal);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void moderateContent_lockHiddenQuestion_throwsConflictWithoutPersistingAction() {
        when(communityQuestionRepository.lockIfApproved(TARGET_ID_1)).thenReturn(0);

        ModerationException ex = assertThrows(ModerationException.class, () -> moderationService.moderateContent(
                new com.carebridge.backend.content.dto.request.ModerateContentRequest(
                        TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.LOCK, "Đã xử lý xong"),
                moderatorPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-031");
        verify(moderationActionRepository, times(0)).save(any());
    }

    @Test
    void moderateContent_lockApprovedQuestion_recordsActionAfterAtomicTransition() {
        UUID moderatorId = UUID.fromString(moderatorPrincipal.getName());
        ModerationAction savedAction = makeAction(UUID.randomUUID(), TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.LOCK, moderatorId, "Đóng thảo luận sau khi đã được xử lý");
        when(communityQuestionRepository.lockIfApproved(TARGET_ID_1)).thenReturn(1);
        when(moderationActionRepository.save(any())).thenReturn(savedAction);

        var response = moderationService.moderateContent(
                new com.carebridge.backend.content.dto.request.ModerateContentRequest(
                        TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.LOCK,
                        "Đóng thảo luận sau khi đã được xử lý"),
                moderatorPrincipal);

        assertThat(response.resultingStatus()).isEqualTo("LOCKED");
        verify(communityQuestionRepository, times(1)).lockIfApproved(TARGET_ID_1);
        verify(moderationActionRepository, times(1)).save(any());
        verify(auditService, times(1)).log(eq(AuditAction.MODERATION_ACTION), eq(moderatorId),
                eq("QUESTION"), eq(TARGET_ID_1.toString()), any());
    }

    private CommunityQuestion makeQuestion(UUID id, QuestionStatus status, UUID authorId, boolean anonymous, int bodyLength) {
        return CommunityQuestion.builder()
                .id(id)
                .topicId(UUID.randomUUID())
                .authorId(authorId)
                .title("Tieu de cau hoi")
                .body("x".repeat(bodyLength))
                .stage(com.carebridge.backend.community.entity.PregnancyStage.PREGNANCY)
                .urgency(com.carebridge.backend.community.entity.UrgencyLevel.LOW)
                .status(status)
                .anonymous(anonymous)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private CommunityAnswer makeAnswer(UUID id, UUID questionId, AnswerStatus status, UUID authorId, int bodyLength) {
        return CommunityAnswer.builder()
                .id(id)
                .questionId(questionId)
                .authorId(authorId)
                .body("y".repeat(bodyLength))
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // DETAIL-TC-001: QUESTION PENDING, body 250 chars — not truncated at 200 like contentPreview
    @Test
    void getContentDetail_questionPending_returnsFullBodyUntruncated() {
        UUID authorId = UUID.randomUUID();
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.PENDING, authorId, false, 250);
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));
        when(userRepository.findById(authorId)).thenReturn(
                java.util.Optional.of(User.builder().id(authorId).name("Nguyen Thi A").build()));

        ModerationContentDetailResponse response =
                moderationService.getContentDetail(ReportTargetType.QUESTION, TARGET_ID_1, mockPrincipal);

        assertThat(response.body()).hasSize(250);
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.title()).isEqualTo("Tieu de cau hoi");
    }

    // DETAIL-TC-002: QUESTION HIDDEN still readable — unlike CommunityQuestionService.getQuestionDetail()
    @Test
    void getContentDetail_questionHidden_isReadableNotFiltered() {
        UUID authorId = UUID.randomUUID();
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.HIDDEN, authorId, true, 50);
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));
        when(userRepository.findById(authorId)).thenReturn(
                java.util.Optional.of(User.builder().id(authorId).name("Nguyen Thi A").build()));

        ModerationContentDetailResponse response =
                moderationService.getContentDetail(ReportTargetType.QUESTION, TARGET_ID_1, mockPrincipal);

        assertThat(response.status()).isEqualTo("HIDDEN");
    }

    // DETAIL-TC-003: QUESTION LOCKED still readable
    @Test
    void getContentDetail_questionLocked_isReadable() {
        UUID authorId = UUID.randomUUID();
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.LOCKED, authorId, false, 50);
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));
        when(userRepository.findById(authorId)).thenReturn(
                java.util.Optional.of(User.builder().id(authorId).name("Nguyen Thi A").build()));

        ModerationContentDetailResponse response =
                moderationService.getContentDetail(ReportTargetType.QUESTION, TARGET_ID_1, mockPrincipal);

        assertThat(response.status()).isEqualTo("LOCKED");
    }

    // DETAIL-TC-004: QUESTION APPROVED still readable via moderator endpoint (used by "Đã xử lý" tab)
    @Test
    void getContentDetail_questionApproved_isReadable() {
        UUID authorId = UUID.randomUUID();
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.APPROVED, authorId, false, 50);
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));
        when(userRepository.findById(authorId)).thenReturn(
                java.util.Optional.of(User.builder().id(authorId).name("Nguyen Thi A").build()));

        ModerationContentDetailResponse response =
                moderationService.getContentDetail(ReportTargetType.QUESTION, TARGET_ID_1, mockPrincipal);

        assertThat(response.status()).isEqualTo("APPROVED");
    }

    // DETAIL-TC-005: ANSWER returns questionId/questionTitle of its parent question
    @Test
    void getContentDetail_answer_returnsParentQuestionContext() {
        UUID authorId = UUID.randomUUID();
        UUID questionId = TARGET_ID_1;
        CommunityQuestion parentQuestion = makeQuestion(questionId, QuestionStatus.PENDING, UUID.randomUUID(), false, 50);
        CommunityAnswer answer = makeAnswer(TARGET_ID_2, questionId, AnswerStatus.APPROVED, authorId, 50);
        when(communityAnswerRepository.findById(TARGET_ID_2)).thenReturn(java.util.Optional.of(answer));
        when(communityQuestionRepository.findById(questionId)).thenReturn(java.util.Optional.of(parentQuestion));
        when(userRepository.findById(authorId)).thenReturn(
                java.util.Optional.of(User.builder().id(authorId).name("BS. Tran Van B").build()));

        ModerationContentDetailResponse response =
                moderationService.getContentDetail(ReportTargetType.ANSWER, TARGET_ID_2, mockPrincipal);

        assertThat(response.questionId()).isEqualTo(questionId);
        assertThat(response.questionTitle()).isEqualTo("Tieu de cau hoi");
        assertThat(response.title()).isNull();
    }

    // DETAIL-TC-006: ANSWER body full, not truncated
    @Test
    void getContentDetail_answer_returnsFullBodyUntruncated() {
        UUID authorId = UUID.randomUUID();
        CommunityAnswer answer = makeAnswer(TARGET_ID_2, TARGET_ID_1, AnswerStatus.PENDING, authorId, 300);
        when(communityAnswerRepository.findById(TARGET_ID_2)).thenReturn(java.util.Optional.of(answer));
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.empty());
        when(userRepository.findById(authorId)).thenReturn(java.util.Optional.empty());

        ModerationContentDetailResponse response =
                moderationService.getContentDetail(ReportTargetType.ANSWER, TARGET_ID_2, mockPrincipal);

        assertThat(response.body()).hasSize(300);
    }

    // DETAIL-TC-007: targetId not found → MOD-007
    @Test
    void getContentDetail_targetNotFound_throwsMod007() {
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.empty());

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.getContentDetail(ReportTargetType.QUESTION, TARGET_ID_1, mockPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-007");
    }

    // DETAIL-TC-008: targetType not supported (CONTENT/ACCOUNT/EXPERT/USER) → MOD-023, no repo call
    @Test
    void getContentDetail_unsupportedTargetType_throwsMod023WithoutQuerying() {
        for (ReportTargetType unsupported : List.of(
                ReportTargetType.CONTENT, ReportTargetType.ACCOUNT, ReportTargetType.EXPERT, ReportTargetType.USER)) {
            ModerationException ex = assertThrows(ModerationException.class,
                    () -> moderationService.getContentDetail(unsupported, TARGET_ID_1, mockPrincipal));
            assertThat(ex.getCode()).isEqualTo("MOD-023");
        }
        verify(communityQuestionRepository, times(0)).findById(any());
        verify(communityAnswerRepository, times(0)).findById(any());
    }

    // DETAIL-TC-011: anonymous=true still returns real authorId/authorName (ADR-003)
    @Test
    void getContentDetail_anonymousQuestion_stillReturnsRealAuthor() {
        UUID authorId = UUID.randomUUID();
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.PENDING, authorId, true, 50);
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));
        when(userRepository.findById(authorId)).thenReturn(
                java.util.Optional.of(User.builder().id(authorId).name("Nguyen Thi A").build()));

        ModerationContentDetailResponse response =
                moderationService.getContentDetail(ReportTargetType.QUESTION, TARGET_ID_1, mockPrincipal);

        assertThat(response.authorId()).isEqualTo(authorId);
        assertThat(response.authorName()).isEqualTo("Nguyen Thi A");
        assertThat(response.anonymous()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════
    // CB-MOD-IMP-009 — Undo Moderation Action
    // ═══════════════════════════════════════════════════════════

    private static final UUID ACTION_ID_1 = UUID.fromString("33333333-0000-0000-0000-000000000001");
    // undoModerationAction() calls SecurityUtils.requireCurrentUserId(), which requires a valid UUID
    // string (unlike getModerationQueue()/etc., which read principal.getName() as a plain string) —
    // mockPrincipal = () -> "1" is not a valid UUID, so the happy-path undo tests need this instead.
    private final Principal moderatorPrincipal = UUID.randomUUID()::toString;

    // UNDO-TC-001: Undo APPROVE on QUESTION -> PENDING
    @Test
    void undoModerationAction_approveOnQuestion_setsStatusPending() {
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, UUID.randomUUID(), null);
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.APPROVED, UUID.randomUUID(), false, 50);

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(original));
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));
        when(moderationActionRepository.save(any(ModerationAction.class)))
                .thenAnswer(inv -> {
                    ModerationAction a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        UndoModerationActionResponse response = moderationService.undoModerationAction(ACTION_ID_1, moderatorPrincipal);

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.PENDING);
        assertThat(response.resultingStatus()).isEqualTo("PENDING");
        assertThat(response.originalActionId()).isEqualTo(ACTION_ID_1);
        verify(communityQuestionRepository, times(1)).save(question);
    }

    // UNDO-TC-002: Undo HIDE on QUESTION -> PENDING
    @Test
    void undoModerationAction_hideOnQuestion_setsStatusPending() {
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.HIDE, UUID.randomUUID(), "spam");
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.HIDDEN, UUID.randomUUID(), false, 50);

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(original));
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.undoModerationAction(ACTION_ID_1, moderatorPrincipal);

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.PENDING);
    }

    // UNDO-TC-003: Undo LOCK on QUESTION -> PENDING
    @Test
    void undoModerationAction_lockOnQuestion_setsStatusPending() {
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.LOCK, UUID.randomUUID(), "locked for review");
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.LOCKED, UUID.randomUUID(), false, 50);

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(original));
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.undoModerationAction(ACTION_ID_1, moderatorPrincipal);

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.PENDING);
    }

    // UNDO-TC-004: Undo APPROVE on ANSWER -> decrements answer_count exactly once (ADR-003)
    @Test
    void undoModerationAction_approveOnAnswer_decrementsAnswerCountOnce() {
        UUID questionId = UUID.randomUUID();
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_2, ReportTargetType.ANSWER,
                ModerationActionType.APPROVE, UUID.randomUUID(), null);
        CommunityAnswer answer = makeAnswer(TARGET_ID_2, questionId, AnswerStatus.APPROVED, UUID.randomUUID(), 50);

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_2, ReportTargetType.ANSWER, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(original));
        when(communityAnswerRepository.findById(TARGET_ID_2)).thenReturn(java.util.Optional.of(answer));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.undoModerationAction(ACTION_ID_1, moderatorPrincipal);

        assertThat(answer.getStatus()).isEqualTo(AnswerStatus.PENDING);
        verify(communityQuestionRepository, times(1)).decrementAnswerCount(questionId);
        verify(communityQuestionRepository, times(0)).incrementAnswerCount(any());
    }

    // UNDO-TC-005: Undo HIDE on ANSWER (never APPROVED) -> answer_count untouched (ADR-003)
    @Test
    void undoModerationAction_hideOnAnswer_doesNotTouchAnswerCount() {
        UUID questionId = UUID.randomUUID();
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_2, ReportTargetType.ANSWER,
                ModerationActionType.HIDE, UUID.randomUUID(), "spam");
        CommunityAnswer answer = makeAnswer(TARGET_ID_2, questionId, AnswerStatus.HIDDEN, UUID.randomUUID(), 50);

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_2, ReportTargetType.ANSWER, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(original));
        when(communityAnswerRepository.findById(TARGET_ID_2)).thenReturn(java.util.Optional.of(answer));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.undoModerationAction(ACTION_ID_1, moderatorPrincipal);

        verify(communityQuestionRepository, times(0)).decrementAnswerCount(any());
        verify(communityQuestionRepository, times(0)).incrementAnswerCount(any());
    }

    // UNDO-TC-006: guard "most recent" fails -> MOD-029, no mutation
    @Test
    void undoModerationAction_notMostRecentAction_throwsMod029WithoutMutating() {
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, UUID.randomUUID(), null);
        ModerationAction newer = makeAction(UUID.randomUUID(), TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.HIDE, UUID.randomUUID(), "later action");

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(newer));

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.undoModerationAction(ACTION_ID_1, mockPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-029");
        verify(communityQuestionRepository, times(0)).save(any());
        verify(moderationActionRepository, times(0)).save(any());
    }

    // UNDO-TC-007: guard "status khớp" fails (target already superseded by a self-edit) -> MOD-030
    @Test
    void undoModerationAction_statusSuperseded_throwsMod030WithoutMutating() {
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, UUID.randomUUID(), null);
        // status is PENDING, not APPROVED as the action would imply — simulates an untracked self-edit
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.PENDING, UUID.randomUUID(), false, 50);

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(original));
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.undoModerationAction(ACTION_ID_1, mockPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-030");
        verify(communityQuestionRepository, times(0)).save(any());
        verify(moderationActionRepository, times(0)).save(any());
    }

    // UNDO-TC-008: action from resolveReport() (reportId != null) -> 400 MOD-027 (ADR-004)
    @Test
    void undoModerationAction_reportResolutionOrigin_throwsMod027() {
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.HIDE, UUID.randomUUID(), "reported content");
        original.setReportId(UUID.randomUUID());

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.undoModerationAction(ACTION_ID_1, mockPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-027");
        verify(contentReportRepository, times(0)).findById(any());
    }

    // UNDO-TC-009: actionType=REQUEST_REVISION cannot be undone -> 400 MOD-028 (ADR-001)
    @Test
    void undoModerationAction_requestRevisionActionType_throwsMod028() {
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.REQUEST_REVISION, UUID.randomUUID(), "please fix");

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.undoModerationAction(ACTION_ID_1, mockPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-028");
    }

    // UNDO-TC-010: targetType=ACCOUNT (WARN/SUSPEND/RESTRICT) rejected by targetType guard -> MOD-026,
    // not MOD-028 — targetType is validated before actionType (fixed guard order)
    @Test
    void undoModerationAction_accountActionTypes_throwMod026NotMod028() {
        for (ModerationActionType accountActionType : List.of(
                ModerationActionType.WARN, ModerationActionType.SUSPEND, ModerationActionType.RESTRICT)) {
            UUID actionId = UUID.randomUUID();
            ModerationAction original = makeAction(actionId, UUID.randomUUID(), ReportTargetType.ACCOUNT,
                    accountActionType, UUID.randomUUID(), "reason");
            when(moderationActionRepository.findById(actionId)).thenReturn(java.util.Optional.of(original));

            ModerationException ex = assertThrows(ModerationException.class,
                    () -> moderationService.undoModerationAction(actionId, mockPrincipal));

            assertThat(ex.getCode()).isEqualTo("MOD-026");
        }
    }

    // UNDO-TC-011: targetType=ACCOUNT -> 400 MOD-026
    @Test
    void undoModerationAction_accountTargetType_throwsMod026() {
        ModerationAction original = makeAction(ACTION_ID_1, UUID.randomUUID(), ReportTargetType.ACCOUNT,
                ModerationActionType.WARN, UUID.randomUUID(), "reason");
        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.undoModerationAction(ACTION_ID_1, mockPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-026");
    }

    // UNDO-TC-012: actionType=UNDO cannot itself be undone -> 400 MOD-028 (no UNDO-of-UNDO loop)
    @Test
    void undoModerationAction_alreadyAnUndoAction_throwsMod028() {
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.UNDO, UUID.randomUUID(), "Hoàn tác hành động APPROVE");

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.undoModerationAction(ACTION_ID_1, mockPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-028");
    }

    // UNDO-TC-013: actionId does not exist -> 404 MOD-025
    @Test
    void undoModerationAction_actionNotFound_throwsMod025() {
        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.empty());

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.undoModerationAction(ACTION_ID_1, mockPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-025");
    }

    // UNDO-TC-014: append-only invariant — a NEW ModerationAction(actionType=UNDO) is saved,
    // the original action object is never mutated (ADR-005, BR-MOD-021)
    @Test
    void undoModerationAction_appendOnly_savesNewActionWithoutMutatingOriginal() {
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, UUID.randomUUID(), null);
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.APPROVED, UUID.randomUUID(), false, 50);

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(original));
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));

        ArgumentCaptor<ModerationAction> savedCaptor = forClass(ModerationAction.class);
        when(moderationActionRepository.save(savedCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        moderationService.undoModerationAction(ACTION_ID_1, moderatorPrincipal);

        ModerationAction saved = savedCaptor.getValue();
        assertThat(saved.getActionType()).isEqualTo(ModerationActionType.UNDO);
        assertThat(saved.getReportId()).isNull();
        // The original action object must retain its own actionType — it is never mutated in place
        assertThat(original.getActionType()).isEqualTo(ModerationActionType.APPROVE);
        assertThat(saved).isNotSameAs(original);
    }

    // ═══════════════════════════════════════════════════════════
    // CB-MOD-IMP-015 — Revert Report Resolution
    // ═══════════════════════════════════════════════════════════

    private ContentReport makeReportWithResolution(ReportStatus status, ReportTargetType targetType, UUID targetId) {
        ContentReport r = new ContentReport();
        r.setId(REPORT_ID_1);
        r.setTargetId(targetId);
        r.setTargetType(targetType);
        r.setStatus(status);
        r.setCategory("Inappropriate content");
        r.setCreatedAt(Instant.now());
        r.setResolvedAt(Instant.parse("2026-07-15T00:00:00Z"));
        r.setAssignedModeratorId(UUID.randomUUID());
        return r;
    }

    // MRR-TC-001: revert DISMISSED report -> PENDING, no linked ModerationAction, no target mutation
    @Test
    void revertReport_dismissedReport_setsStatusPendingWithoutAction() {
        ContentReport report = makeReportWithResolution(ReportStatus.DISMISSED, ReportTargetType.ANSWER, TARGET_ID_2);
        Instant originalResolvedAt = report.getResolvedAt();
        UUID originalAssignedModeratorId = report.getAssignedModeratorId();

        when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.of(report));
        when(moderationActionRepository.findTopByReportIdAndActionTypeNotOrderByActionAtDesc(REPORT_ID_1, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.empty());
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        RevertReportResponse response = moderationService.revertReport(
                REPORT_ID_1, new RevertReportRequest(null), moderatorPrincipal);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getRevertedAt()).isNotNull();
        assertThat(report.getResolvedAt()).isEqualTo(originalResolvedAt);
        assertThat(report.getAssignedModeratorId()).isEqualTo(originalAssignedModeratorId);
        assertThat(response.undoActionId()).isNull();
        assertThat(response.resultingStatus()).isNull();
        verify(communityAnswerRepository, times(0)).save(any());
        verify(communityQuestionRepository, times(0)).save(any());
    }

    // MRR-TC-002: revert RESOLVED report (outcome=HIDE on ANSWER) -> target back to PENDING,
    // resolvedAt/assignedModeratorId untouched, answer_count untouched (HIDE never counted)
    @Test
    void revertReport_resolvedHideOnAnswer_setsTargetAndReportPending() {
        ContentReport report = makeReportWithResolution(ReportStatus.RESOLVED, ReportTargetType.ANSWER, TARGET_ID_2);
        Instant originalResolvedAt = report.getResolvedAt();
        UUID originalAssignedModeratorId = report.getAssignedModeratorId();
        ModerationAction linkedAction = makeAction(ACTION_ID_1, TARGET_ID_2, ReportTargetType.ANSWER,
                ModerationActionType.HIDE, UUID.randomUUID(), "reported");
        linkedAction.setReportId(REPORT_ID_1);
        CommunityAnswer answer = makeAnswer(TARGET_ID_2, UUID.randomUUID(), AnswerStatus.HIDDEN, UUID.randomUUID(), 40);

        when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.of(report));
        when(moderationActionRepository.findTopByReportIdAndActionTypeNotOrderByActionAtDesc(REPORT_ID_1, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(linkedAction));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_2, ReportTargetType.ANSWER, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(linkedAction));
        when(communityAnswerRepository.findById(TARGET_ID_2)).thenReturn(java.util.Optional.of(answer));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> {
            ModerationAction a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        RevertReportResponse response = moderationService.revertReport(
                REPORT_ID_1, new RevertReportRequest("bấm nhầm"), moderatorPrincipal);

        assertThat(answer.getStatus()).isEqualTo(AnswerStatus.PENDING);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getResolvedAt()).isEqualTo(originalResolvedAt);
        assertThat(report.getAssignedModeratorId()).isEqualTo(originalAssignedModeratorId);
        assertThat(response.undoActionId()).isNotNull();
        assertThat(response.resultingStatus()).isEqualTo("PENDING");
        verify(communityQuestionRepository, times(0)).decrementAnswerCount(any());
        verify(communityQuestionRepository, times(0)).incrementAnswerCount(any());
    }

    // MRR-TC-003: revert RESOLVED report (outcome=APPROVE on QUESTION) -> target back to PENDING
    @Test
    void revertReport_resolvedApproveOnQuestion_setsTargetPending() {
        ContentReport report = makeReportWithResolution(ReportStatus.RESOLVED, ReportTargetType.QUESTION, TARGET_ID_1);
        ModerationAction linkedAction = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.APPROVE, UUID.randomUUID(), null);
        linkedAction.setReportId(REPORT_ID_1);
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.APPROVED, UUID.randomUUID(), false, 50);

        when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.of(report));
        when(moderationActionRepository.findTopByReportIdAndActionTypeNotOrderByActionAtDesc(REPORT_ID_1, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(linkedAction));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(linkedAction));
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.revertReport(REPORT_ID_1, new RevertReportRequest(null), moderatorPrincipal);

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.PENDING);
    }

    // MRR-TC-004: revert RESOLVED report (outcome=LOCK on QUESTION) -> target back to PENDING
    @Test
    void revertReport_resolvedLockOnQuestion_setsTargetPending() {
        ContentReport report = makeReportWithResolution(ReportStatus.RESOLVED, ReportTargetType.QUESTION, TARGET_ID_1);
        ModerationAction linkedAction = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.LOCK, UUID.randomUUID(), "locked");
        linkedAction.setReportId(REPORT_ID_1);
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.LOCKED, UUID.randomUUID(), false, 50);

        when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.of(report));
        when(moderationActionRepository.findTopByReportIdAndActionTypeNotOrderByActionAtDesc(REPORT_ID_1, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(linkedAction));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(linkedAction));
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.revertReport(REPORT_ID_1, new RevertReportRequest(null), moderatorPrincipal);

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.PENDING);
    }

    // MRR-TC-005: report does not exist -> 404 MOD-003
    @Test
    void revertReport_reportNotFound_throwsMod003() {
        when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.empty());

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.revertReport(REPORT_ID_1, new RevertReportRequest(null), moderatorPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-003");
    }

    // MRR-TC-006: report is still PENDING -> 400 MOD-032, no mutation
    @Test
    void revertReport_reportStillPending_throwsMod032WithoutMutating() {
        ContentReport report = makeReportWithResolution(ReportStatus.PENDING, ReportTargetType.QUESTION, TARGET_ID_1);
        when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.of(report));

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.revertReport(REPORT_ID_1, new RevertReportRequest(null), moderatorPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-032");
        verify(contentReportRepository, times(0)).save(any());
        verify(moderationActionRepository, times(0)).findTopByReportIdAndActionTypeNotOrderByActionAtDesc(any(), any());
    }

    // MRR-TC-007..009: linked action is an account-level outcome -> 400 MOD-033, no mutation
    @Test
    void revertReport_accountLevelLinkedAction_throwsMod033() {
        for (ModerationActionType accountActionType : List.of(
                ModerationActionType.WARN, ModerationActionType.SUSPEND, ModerationActionType.RESTRICT)) {
            ContentReport report = makeReportWithResolution(ReportStatus.RESOLVED, ReportTargetType.USER, UUID.randomUUID());
            ModerationAction linkedAction = makeAction(UUID.randomUUID(), UUID.randomUUID(), ReportTargetType.ACCOUNT,
                    accountActionType, UUID.randomUUID(), "reason");

            when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.of(report));
            when(moderationActionRepository.findTopByReportIdAndActionTypeNotOrderByActionAtDesc(REPORT_ID_1, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                    .thenReturn(java.util.Optional.of(linkedAction));

            ModerationException ex = assertThrows(ModerationException.class,
                    () -> moderationService.revertReport(REPORT_ID_1, new RevertReportRequest(null), moderatorPrincipal));

            assertThat(ex.getCode()).isEqualTo("MOD-033");
            verify(contentReportRepository, times(0)).save(any());
        }
    }

    // MRR-TC-010: linked action is not the most recent action on its target -> 409 MOD-034
    @Test
    void revertReport_linkedActionNotMostRecent_throwsMod034() {
        ContentReport report = makeReportWithResolution(ReportStatus.RESOLVED, ReportTargetType.QUESTION, TARGET_ID_1);
        ModerationAction linkedAction = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.HIDE, UUID.randomUUID(), "reported");
        linkedAction.setReportId(REPORT_ID_1);
        ModerationAction newerDirectAction = makeAction(UUID.randomUUID(), TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.LOCK, UUID.randomUUID(), "locked after report resolved");

        when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.of(report));
        when(moderationActionRepository.findTopByReportIdAndActionTypeNotOrderByActionAtDesc(REPORT_ID_1, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(linkedAction));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(newerDirectAction));

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.revertReport(REPORT_ID_1, new RevertReportRequest(null), moderatorPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-034");
        verify(communityQuestionRepository, times(0)).save(any());
        verify(contentReportRepository, times(0)).save(any());
    }

    // MRR-TC-011: target's current status no longer matches the linked action's result -> 409 MOD-035
    @Test
    void revertReport_targetStatusSuperseded_throwsMod035() {
        ContentReport report = makeReportWithResolution(ReportStatus.RESOLVED, ReportTargetType.QUESTION, TARGET_ID_1);
        ModerationAction linkedAction = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.HIDE, UUID.randomUUID(), "reported");
        linkedAction.setReportId(REPORT_ID_1);
        // status is PENDING, not HIDDEN as the linked action would imply — simulates an untracked change
        CommunityQuestion question = makeQuestion(TARGET_ID_1, QuestionStatus.PENDING, UUID.randomUUID(), false, 50);

        when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.of(report));
        when(moderationActionRepository.findTopByReportIdAndActionTypeNotOrderByActionAtDesc(REPORT_ID_1, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(linkedAction));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_1, ReportTargetType.QUESTION, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(linkedAction));
        when(communityQuestionRepository.findById(TARGET_ID_1)).thenReturn(java.util.Optional.of(question));

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.revertReport(REPORT_ID_1, new RevertReportRequest(null), moderatorPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-035");
        verify(communityQuestionRepository, times(0)).save(any());
        verify(contentReportRepository, times(0)).save(any());
    }

    // MRR-TC-012: resolvedAt/assignedModeratorId are never overwritten by revert (ADR-005/BR-MOD-018)
    @Test
    void revertReport_neverOverwritesOriginalResolutionFields() {
        ContentReport report = makeReportWithResolution(ReportStatus.RESOLVED, ReportTargetType.ANSWER, TARGET_ID_2);
        Instant originalResolvedAt = report.getResolvedAt();
        UUID originalAssignedModeratorId = report.getAssignedModeratorId();
        ModerationAction linkedAction = makeAction(ACTION_ID_1, TARGET_ID_2, ReportTargetType.ANSWER,
                ModerationActionType.HIDE, UUID.randomUUID(), "reported");
        linkedAction.setReportId(REPORT_ID_1);
        CommunityAnswer answer = makeAnswer(TARGET_ID_2, UUID.randomUUID(), AnswerStatus.HIDDEN, UUID.randomUUID(), 40);

        when(contentReportRepository.findById(REPORT_ID_1)).thenReturn(java.util.Optional.of(report));
        when(moderationActionRepository.findTopByReportIdAndActionTypeNotOrderByActionAtDesc(REPORT_ID_1, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(linkedAction));
        when(moderationActionRepository.findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(TARGET_ID_2, ReportTargetType.ANSWER, ModerationActionType.AI_FEEDBACK_SUBMITTED))
                .thenReturn(java.util.Optional.of(linkedAction));
        when(communityAnswerRepository.findById(TARGET_ID_2)).thenReturn(java.util.Optional.of(answer));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<ContentReport> savedReportCaptor = forClass(ContentReport.class);
        when(contentReportRepository.save(savedReportCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        moderationService.revertReport(REPORT_ID_1, new RevertReportRequest(null), moderatorPrincipal);

        ContentReport saved = savedReportCaptor.getValue();
        assertThat(saved.getResolvedAt()).isEqualTo(originalResolvedAt);
        assertThat(saved.getAssignedModeratorId()).isEqualTo(originalAssignedModeratorId);
        assertThat(saved.getRevertedAt()).isNotNull();
        assertThat(saved.getRevertedBy()).isEqualTo(UUID.fromString(moderatorPrincipal.getName()));
    }

    // MRR-TC-015 (regression): undoModerationAction()/MOD-027 is unaffected by revertReport() existing
    @Test
    void undoModerationAction_stillRejectsReportOriginatedAction_afterRevertFeatureAdded() {
        ModerationAction original = makeAction(ACTION_ID_1, TARGET_ID_1, ReportTargetType.QUESTION,
                ModerationActionType.HIDE, UUID.randomUUID(), "reported content");
        original.setReportId(UUID.randomUUID());

        when(moderationActionRepository.findById(ACTION_ID_1)).thenReturn(java.util.Optional.of(original));

        ModerationException ex = assertThrows(ModerationException.class,
                () -> moderationService.undoModerationAction(ACTION_ID_1, mockPrincipal));

        assertThat(ex.getCode()).isEqualTo("MOD-027");
        verify(contentReportRepository, times(0)).findById(any());
    }
}
