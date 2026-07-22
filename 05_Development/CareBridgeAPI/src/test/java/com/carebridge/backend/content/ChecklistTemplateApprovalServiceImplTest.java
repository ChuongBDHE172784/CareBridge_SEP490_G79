package com.carebridge.backend.content;

import static com.carebridge.backend.content.ChecklistTemplateTestFactory.ADMIN_ID;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.TEMPLATE_ID;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.makeTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.service.ChecklistTemplateApprovalServiceImpl;
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

    @InjectMocks
    private ChecklistTemplateApprovalServiceImpl service;

    private final Principal principal = () -> ADMIN_ID.toString();

    // CHKTPL-TC-010
    @Test
    void decide_approvePendingReview_transitionsToApproved() {
        ChecklistTemplate template = makeTemplate(t -> t.setStatus(ContentStatus.PENDING_REVIEW));
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        ChecklistTemplateDecisionResponse response = service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal);

        assertEquals(ContentStatus.APPROVED, response.newStatus());
        verify(auditService).log(eq(AuditAction.CHECKLIST_TEMPLATE_DECIDED), eq(ADMIN_ID), eq("ChecklistTemplate"), any(), any());
    }

    // CHKTPL-TC-011
    @Test
    void decide_rejectPendingReview_transitionsToDraftWithReason() {
        ChecklistTemplate template = makeTemplate(t -> t.setStatus(ContentStatus.PENDING_REVIEW));
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        ChecklistTemplateDecisionResponse response = service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.REJECT, "Thiếu mục quan trọng"), principal);

        assertEquals(ContentStatus.DRAFT, response.newStatus());
        assertEquals("Thiếu mục quan trọng", response.reason());
    }

    // CHKTPL-TC-012a
    @ParameterizedTest
    @MethodSource("nonPendingReviewStatuses")
    void decide_nonPendingReviewStatus_throwsChktpl007(ContentStatus status) {
        ChecklistTemplate template = makeTemplate(t -> t.setStatus(status));
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        ContentException ex = assertThrows(ContentException.class, () -> service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal));

        assertEquals("CHKTPL-007", ex.getCode());
        verify(checklistTemplateRepository, never()).save(any());
    }

    private static Stream<ContentStatus> nonPendingReviewStatuses() {
        return Stream.of(ContentStatus.DRAFT, ContentStatus.APPROVED, ContentStatus.ARCHIVED);
    }

    // CHKTPL-TC-012b
    @ParameterizedTest
    @MethodSource("blankReasons")
    void decide_rejectMissingReason_throwsChktpl008(String reason) {
        ChecklistTemplate template = makeTemplate(t -> t.setStatus(ContentStatus.PENDING_REVIEW));
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
