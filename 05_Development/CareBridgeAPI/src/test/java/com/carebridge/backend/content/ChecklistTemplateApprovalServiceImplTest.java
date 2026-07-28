package com.carebridge.backend.content;

import static com.carebridge.backend.content.ChecklistTemplateTestFactory.ADMIN_ID;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.TEMPLATE_ID;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.makeTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateDecisionResponse;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentDecision;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.service.ChecklistTemplateApprovalServiceImpl;
import com.carebridge.backend.notification.service.ContentReviewNotificationService;
import java.security.Principal;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistTemplateApprovalServiceImplTest {

    @Mock
    private ChecklistTemplateRepository checklistTemplateRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ContentReviewNotificationService contentReviewNotificationService;

    @InjectMocks
    private ChecklistTemplateApprovalServiceImpl service;

    private final Principal principal = () -> ADMIN_ID.toString();

    // CHKTPL-TC-010
    @Test
    void decide_approvePendingReview_transitionsToApproved() {
        ChecklistTemplate template = makeTemplate(t -> t.setStatus(ChecklistTemplateStatus.PENDING_REVIEW));
        template.setRevisionReason("Phản hồi cũ");
        template.setRevisionRequestedAt(java.time.Instant.now());
        template.setRevisionRequestedBy(ADMIN_ID);
        template.setRevisionRequestedVersion(1);
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        ChecklistTemplateDecisionResponse response = service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal);

        assertEquals(ChecklistTemplateStatus.APPROVED, response.newStatus());
        assertEquals(null, template.getRevisionReason());
        assertEquals(null, template.getRevisionRequestedAt());
        assertEquals(null, template.getRevisionRequestedBy());
        assertEquals(null, template.getRevisionRequestedVersion());
        verify(auditService).log(eq(AuditAction.CHECKLIST_TEMPLATE_DECIDED), eq(ADMIN_ID), eq("ChecklistTemplate"), any(), any());
    }

    // CHKTPL-TC-011
    @Test
    void decide_rejectPendingReview_transitionsToDraftWithReason() {
        ChecklistTemplate template = makeTemplate(t -> t.setStatus(ChecklistTemplateStatus.PENDING_REVIEW));
        template.setAuthorUserId(ADMIN_ID);
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        ChecklistTemplateDecisionResponse response = service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.REJECT, "Thiếu mục quan trọng"), principal);

        assertEquals(ChecklistTemplateStatus.DRAFT, response.newStatus());
        assertEquals("Thiếu mục quan trọng", response.reason());
        assertEquals("Thiếu mục quan trọng", template.getRevisionReason());
        assertEquals(ADMIN_ID, template.getRevisionRequestedBy());
        assertEquals(template.getVersionNo(), template.getRevisionRequestedVersion());
        assertNotNull(template.getRevisionRequestedAt());
        verify(contentReviewNotificationService).notifyReturned(
                eq(ADMIN_ID), eq(TEMPLATE_ID), eq("CHECKLIST"), eq(template.getName()),
                eq("Thiếu mục quan trọng"), eq("/content/checklists/" + TEMPLATE_ID + "/edit"));
    }

    // CHKTPL-TC-012a
    @ParameterizedTest
    @MethodSource("nonPendingReviewStatuses")
    void decide_nonPendingReviewStatus_throwsChktpl007(ChecklistTemplateStatus status) {
        ChecklistTemplate template = makeTemplate(t -> t.setStatus(status));
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        ContentException ex = assertThrows(ContentException.class, () -> service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal));

        assertEquals("CHKTPL-007", ex.getCode());
        verify(checklistTemplateRepository, never()).save(any());
    }

    private static Stream<ChecklistTemplateStatus> nonPendingReviewStatuses() {
        return Stream.of(
                ChecklistTemplateStatus.DRAFT,
                ChecklistTemplateStatus.REJECTED,
                ChecklistTemplateStatus.APPROVED,
                ChecklistTemplateStatus.ARCHIVED);
    }

    // CHKTPL-TC-012b
    @ParameterizedTest
    @MethodSource("blankReasons")
    void decide_rejectMissingReason_throwsChktpl008(String reason) {
        ChecklistTemplate template = makeTemplate(t -> t.setStatus(ChecklistTemplateStatus.PENDING_REVIEW));
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        ContentException ex = assertThrows(ContentException.class, () -> service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.REJECT, reason), principal));

        assertEquals("CHKTPL-008", ex.getCode());
        verify(checklistTemplateRepository, never()).save(any());
    }

    private static Stream<String> blankReasons() {
        return Stream.of(null, "  ");
    }

    @Test
    void decide_notFound_throwsChktpl003() {
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());

        ContentException ex = assertThrows(ContentException.class, () -> service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal));

        assertEquals("CHKTPL-003", ex.getCode());
    }
}
