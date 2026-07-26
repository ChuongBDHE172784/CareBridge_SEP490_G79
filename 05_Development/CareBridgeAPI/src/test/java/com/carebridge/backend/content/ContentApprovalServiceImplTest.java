package com.carebridge.backend.content;

import static com.carebridge.backend.content.ApproveContentVersionTestFactory.ADMIN_ID;
import static com.carebridge.backend.content.ApproveContentVersionTestFactory.CONTENT_ID;
import static com.carebridge.backend.content.ApproveContentVersionTestFactory.makeItem;
import static com.carebridge.backend.content.ApproveContentVersionTestFactory.makeRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.dto.response.ContentDecisionResponse;
import com.carebridge.backend.content.entity.ContentDecision;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.service.ContentApprovalServiceImpl;
import java.security.Principal;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// CAV-TC-1001 .. CAV-TC-1007 (CB-CONTENT-TEST-005 §4)
@ExtendWith(MockitoExtension.class)
class ContentApprovalServiceImplTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private com.carebridge.backend.aimoderation.service.AiScanEnqueueService aiScanEnqueueService;

    @InjectMocks
    private ContentApprovalServiceImpl contentApprovalService;

    private final Principal principal = () -> ADMIN_ID.toString();

    // CAV-TC-1001
    @Test
    void decide_approvePendingReview_transitionsToApproved() {
        ContentItem item = makeItem(ContentStatus.PENDING_REVIEW, 3);
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentDecisionResponse response = contentApprovalService.decide(
                CONTENT_ID, makeRequest(ContentDecision.APPROVE, null), principal);

        assertEquals(ContentStatus.APPROVED, response.newStatus());
        assertEquals(3, response.versionNoAtDecision());
    }

    // CAV-TC-1002
    @Test
    void decide_rejectPendingReview_transitionsToDraftWithReason() {
        ContentItem item = makeItem(ContentStatus.PENDING_REVIEW, 3);
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentDecisionResponse response = contentApprovalService.decide(
                CONTENT_ID, makeRequest(ContentDecision.REJECT, "Thiếu trích dẫn"), principal);

        assertEquals(ContentStatus.DRAFT, response.newStatus());
        assertEquals("Thiếu trích dẫn", response.reason());
    }

    // CAV-TC-1003 (CRITICAL — transition guard)
    @ParameterizedTest
    @MethodSource("nonPendingReviewStatuses")
    void decide_nonPendingReviewStatus_throwsCnt008(ContentStatus status) {
        ContentItem item = makeItem(status, 1);
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(item));

        ContentException ex = assertThrows(ContentException.class,
                () -> contentApprovalService.decide(CONTENT_ID, makeRequest(ContentDecision.APPROVE, null), principal));

        assertEquals("CNT-008", ex.getCode());
        verify(contentRepository, never()).save(any());
    }

    private static Stream<ContentStatus> nonPendingReviewStatuses() {
        return Stream.of(ContentStatus.DRAFT, ContentStatus.APPROVED, ContentStatus.ARCHIVED);
    }

    // CAV-TC-1004
    @Test
    void decide_notFound_throwsCnt003() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

        ContentException ex = assertThrows(ContentException.class,
                () -> contentApprovalService.decide(CONTENT_ID, makeRequest(ContentDecision.APPROVE, null), principal));

        assertEquals("CNT-003", ex.getCode());
        verify(contentRepository, never()).save(any());
    }

    // CAV-TC-1005
    @ParameterizedTest
    @MethodSource("blankReasons")
    void decide_rejectMissingReason_throwsCnt009(String reason) {
        ContentItem item = makeItem(ContentStatus.PENDING_REVIEW, 1);
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(item));

        ContentException ex = assertThrows(ContentException.class,
                () -> contentApprovalService.decide(CONTENT_ID, makeRequest(ContentDecision.REJECT, reason), principal));

        assertEquals("CNT-009", ex.getCode());
        verify(contentRepository, never()).save(any());
    }

    private static Stream<String> blankReasons() {
        return Stream.of(null, "  ");
    }

    // CAV-TC-1006 (CRITICAL — ADR-001 scope guard: versionNoAtDecision reflects the CURRENT row value
    // only, read directly off ContentItem; the service has no dependency on any version-history
    // repository/table, so no such lookup can be invoked)
    @Test
    void decide_versionNoAtDecision_reflectsCurrentVersionNoOnly() {
        ContentItem item = makeItem(ContentStatus.PENDING_REVIEW, 7);
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentDecisionResponse response = contentApprovalService.decide(
                CONTENT_ID, makeRequest(ContentDecision.APPROVE, null), principal);

        assertEquals(7, response.versionNoAtDecision());
    }

    // CAV-TC-1007
    @Test
    void decide_onSuccess_auditServiceLogCalledOnceWithContentDecided() {
        ContentItem item = makeItem(ContentStatus.PENDING_REVIEW, 3);
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        contentApprovalService.decide(CONTENT_ID, makeRequest(ContentDecision.APPROVE, null), principal);

        verify(auditService).log(
                org.mockito.ArgumentMatchers.eq(AuditAction.CONTENT_DECIDED),
                org.mockito.ArgumentMatchers.eq(ADMIN_ID),
                org.mockito.ArgumentMatchers.eq("ContentItem"),
                org.mockito.ArgumentMatchers.eq(CONTENT_ID.toString()),
                any());
    }

    // CAV-TC-1011 (post-implementation addition — TDS §6.1 explicitly specifies
    // "item.setPublishedAt(now()) [if not already set]" on APPROVE; searchByFilters() orders results by
    // publishedAt DESC NULLS LAST, so a null value would sink newly-approved content to the bottom of the
    // public feed instead of surfacing it — found during advisor review, not originally listed in the
    // Test-Spec's 11 TCs)
    @Test
    void decide_approveWithNullPublishedAt_setsPublishedAtToNow() {
        ContentItem item = makeItem(ContentStatus.PENDING_REVIEW, 3);
        Assertions.assertNull(item.getPublishedAt());
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        contentApprovalService.decide(CONTENT_ID, makeRequest(ContentDecision.APPROVE, null), principal);
        Instant after = Instant.now();

        ArgumentCaptor<ContentItem> captor = ArgumentCaptor.forClass(ContentItem.class);
        verify(contentRepository).save(captor.capture());
        Instant publishedAt = captor.getValue().getPublishedAt();
        Assertions.assertNotNull(publishedAt);
        Assertions.assertFalse(publishedAt.isBefore(before));
        Assertions.assertFalse(publishedAt.isAfter(after));
    }

    // CAV-TC-1012 (post-implementation addition — a previously-approved item re-decided is not expected
    // to re-publish; publishedAt should not be overwritten once set)
    @Test
    void decide_approveWithExistingPublishedAt_doesNotOverwrite() {
        ContentItem item = makeItem(ContentStatus.PENDING_REVIEW, 3);
        Instant original = Instant.parse("2026-01-01T00:00:00Z");
        item.setPublishedAt(original);
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        contentApprovalService.decide(CONTENT_ID, makeRequest(ContentDecision.APPROVE, null), principal);

        ArgumentCaptor<ContentItem> captor = ArgumentCaptor.forClass(ContentItem.class);
        verify(contentRepository).save(captor.capture());
        Assertions.assertEquals(original, captor.getValue().getPublishedAt());
    }

    // CAV-TC-1013 (post-implementation addition — BR-AUDIT-001 lists reason as part of the audit record;
    // found during advisor review that the original implementation dropped it from the audit detail,
    // leaving it only in the transient HTTP response)
    @Test
    void decide_rejectWithReason_auditDetailIncludesReason() {
        ContentItem item = makeItem(ContentStatus.PENDING_REVIEW, 3);
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(item));
        when(contentRepository.save(any(ContentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        contentApprovalService.decide(CONTENT_ID, makeRequest(ContentDecision.REJECT, "Thiếu trích dẫn"), principal);

        ArgumentCaptor<Object> detailCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditService).log(
                org.mockito.ArgumentMatchers.eq(AuditAction.CONTENT_DECIDED),
                org.mockito.ArgumentMatchers.eq(ADMIN_ID),
                org.mockito.ArgumentMatchers.eq("ContentItem"),
                org.mockito.ArgumentMatchers.eq(CONTENT_ID.toString()),
                detailCaptor.capture());
        Assertions.assertTrue(detailCaptor.getValue().toString().contains("Thiếu trích dẫn"));
    }
}
