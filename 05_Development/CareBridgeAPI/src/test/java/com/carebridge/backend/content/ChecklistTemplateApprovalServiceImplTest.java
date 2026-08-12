package com.carebridge.backend.content;

import static com.carebridge.backend.content.ChecklistTemplateTestFactory.ADMIN_ID;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.SUBSTAGE_ID;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.TEMPLATE_ID;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.makeTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateDecisionResponse;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ContentDecision;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.content.service.ChecklistTemplateApprovalServiceImpl;
import com.carebridge.backend.notification.service.ContentReviewNotificationService;
import java.security.Principal;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
    private ChecklistAuditWriter checklistAuditWriter;

    @Mock
    private ContentReviewNotificationService contentReviewNotificationService;

    @Mock
    private ChecklistItemRepository checklistItemRepository;

    @Mock
    private ChecklistInstanceRepository checklistInstanceRepository;

    @InjectMocks
    private ChecklistTemplateApprovalServiceImpl service;

    private final Principal principal = () -> ADMIN_ID.toString();

    @BeforeEach
    void stubV2ApprovalContract() {
        lenient().when(checklistItemRepository.findByTemplate_IdOrderByOrder(TEMPLATE_ID))
                .thenReturn(java.util.List.of());
    }

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
        verify(checklistAuditWriter).write(any());
    }

    @Test
    void decide_approveOptionalTemplate_doesNotAutoDistribute() {
        ChecklistTemplate template = makeTemplate(value -> {
            value.setStatus(ChecklistTemplateStatus.PENDING_REVIEW);
            value.setTemplateType(ChecklistTemplateType.OPTIONAL);
        });
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        ChecklistTemplateDecisionResponse response = service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal);

        assertEquals(ChecklistTemplateStatus.APPROVED, response.newStatus());
        assertFalse(template.getDistributionEnabled());
        verify(checklistAuditWriter).write(any());
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

    @Test
    void decide_nullDecisionFailsBeforeMutation() {
        assertThrows(IllegalArgumentException.class, () -> service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(null, null), principal));

        verify(checklistTemplateRepository, never()).save(any());
        verify(checklistAuditWriter, never()).write(any());
    }

    @Test
    void decide_v2RejectsLeafWithLegacyContractMetadata() {
        ChecklistTemplate template = makeTemplate(t -> {
            t.setStatus(ChecklistTemplateStatus.PENDING_REVIEW);
            t.setChecklistContractVersion((short) 2);
        });
        ChecklistItem legacyLeaf = ChecklistItem.builder()
                .template(template)
                .itemText("Legacy target")
                .order(1)
                .checklistContractVersion((short) 1)
                .targetSubject(ChecklistTargetSubject.MOTHER)
                .isRequired(true)
                .build();
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistItemRepository.findByTemplate_IdOrderByOrder(TEMPLATE_ID))
                .thenReturn(List.of(legacyLeaf));

        ContentException error = assertThrows(ContentException.class, () -> service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal));

        assertEquals("CNT-001", error.getCode());
        verify(checklistTemplateRepository, never()).save(any());
        verify(checklistAuditWriter, never()).write(any());
    }

    @Test
    void reviewImported_draftSeedMovesToPendingReviewAndRecordsTechnicalReview() {
        ChecklistTemplate template = makeTemplate(value -> {
            value.setStatus(ChecklistTemplateStatus.DRAFT);
            value.setMigrationReviewRequired(true);
            value.setChecklistContractVersion((short) 2);
        });
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistTemplateDecisionResponse response = service.reviewImported(TEMPLATE_ID, principal);

        assertEquals(ChecklistTemplateStatus.DRAFT, response.previousStatus());
        assertEquals(ChecklistTemplateStatus.PENDING_REVIEW, response.newStatus());
        assertEquals(ChecklistTemplateStatus.PENDING_REVIEW, template.getStatus());
        assertFalse(template.getMigrationReviewRequired());
        assertNotNull(template.getMigrationReviewedAt());
        assertEquals(ADMIN_ID, template.getMigrationReviewedBy());
        assertFalse(template.getDistributionEnabled());
        verify(checklistAuditWriter).write(any());
    }

    @Test
    void activateImported_pendingPregnancyV2ProvenanceFailsBeforeMutation() {
        ChecklistTemplate template = makeTemplate(value -> {
            value.setStatus(ChecklistTemplateStatus.PENDING_REVIEW);
            value.setMigrationReviewRequired(false);
            value.setMigrationReviewedAt(java.time.Instant.now());
            value.setMigrationReviewedBy(ADMIN_ID);
            value.setChecklistContractVersion((short) 2);
            value.setChecklistMetadataJson(
                    "{\"schema\":\"CHECKLIST_METADATA_V1\","
                            + "\"provenanceStatus\":\"PENDING_CLINICAL_COPY_SIGN_OFF\"}");
        });
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        ContentException error = assertThrows(ContentException.class,
                () -> service.activateImported(TEMPLATE_ID, principal));

        assertEquals("CHECKLIST_PROVENANCE_SIGN_OFF_REQUIRED", error.getCode());
        verify(checklistTemplateRepository, never()).save(any());
        verify(checklistAuditWriter, never()).write(any());
    }

    @Test
    void activateImported_signedOffPregnancyV2TransitionsToApprovedAndEnablesDistribution() {
        ChecklistTemplate template = makeTemplate(value -> {
            value.setStatus(ChecklistTemplateStatus.PENDING_REVIEW);
            value.setMigrationReviewRequired(false);
            value.setMigrationReviewedAt(java.time.Instant.now());
            value.setMigrationReviewedBy(ADMIN_ID);
            value.setChecklistContractVersion((short) 2);
            value.setChecklistMetadataJson(
                    "{\"schema\":\"CHECKLIST_METADATA_V1\","
                            + "\"provenanceStatus\":\"SIGNED_OFF\"}");
        });
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistTemplateDecisionResponse response = service.activateImported(TEMPLATE_ID, principal);

        assertEquals(ChecklistTemplateStatus.PENDING_REVIEW, response.previousStatus());
        assertEquals(ChecklistTemplateStatus.APPROVED, response.newStatus());
        assertEquals(ChecklistTemplateStatus.APPROVED, template.getStatus());
        assertEquals(Boolean.TRUE, template.getDistributionEnabled());
        assertEquals(ADMIN_ID, template.getApprovedBy());
        assertNotNull(template.getApprovedAt());
        verify(checklistTemplateRepository).save(template);
        verify(checklistAuditWriter).write(any());
    }

    @Test
    void decide_approveAfterTechnicalReviewUsesStableProvenanceGate() {
        ChecklistTemplate template = makeTemplate(value -> {
            value.setStatus(ChecklistTemplateStatus.PENDING_REVIEW);
            value.setMigrationReviewRequired(false);
            value.setMigrationReviewedAt(java.time.Instant.now());
            value.setMigrationReviewedBy(ADMIN_ID);
            value.setChecklistContractVersion((short) 2);
            value.setChecklistMetadataJson(
                    "{\"schema\":\"CHECKLIST_METADATA_V1\","
                            + "\"provenanceStatus\":\"PENDING_CLINICAL_COPY_SIGN_OFF\"}");
        });
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        ContentException error = assertThrows(ContentException.class,
                () -> service.decide(TEMPLATE_ID,
                        new ContentDecisionRequest(ContentDecision.APPROVE, null), principal));

        assertEquals("CHECKLIST_PROVENANCE_SIGN_OFF_REQUIRED", error.getCode());
        verify(checklistTemplateRepository, never()).save(any());
        verify(checklistAuditWriter, never()).write(any());
    }

    @Test
    void decide_positiveSequenceWithActiveLegacy_rejectsBeforeMutationWithTypedReason() {
        ChecklistTemplate legacy = makeTemplate(template -> {
            template.setStage(com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY);
            template.setEligibilityAnchorType(ChecklistAnchorType.NONE);
            template.setEligibilityRangeUnit(ChecklistRangeUnit.DAY);
            template.setEligibilityStartInclusive(0);
            template.setEligibilityEndInclusive(0);
            template.setStatus(ChecklistTemplateStatus.APPROVED);
            template.setDistributionEnabled(true);
            template.setSequencePosition(0);
        });
        ChecklistTemplate sequence = makeTemplate(template -> {
            template.setStage(com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY);
            template.setEligibilityAnchorType(ChecklistAnchorType.NONE);
            template.setEligibilityRangeUnit(ChecklistRangeUnit.DAY);
            template.setEligibilityStartInclusive(0);
            template.setEligibilityEndInclusive(0);
            template.setStatus(ChecklistTemplateStatus.PENDING_REVIEW);
            template.setSequencePosition(1);
        });
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(sequence));
        when(checklistTemplateRepository.findAllDistributionEnabledByStageAndStatus(
                com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY,
                ChecklistTemplateStatus.APPROVED)).thenReturn(List.of(legacy));
        ContentException ex = assertThrows(ContentException.class, () -> service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal));

        assertEquals("CNT-001", ex.getCode());
        assertEquals(ChecklistTemplateApprovalServiceImpl.ACTIVE_LEGACY_CONFLICT,
                ex.getMetadata().get("reasonCode"));
        verify(checklistTemplateRepository, never()).save(any());
        verify(checklistAuditWriter, never()).write(any());
    }

    @Test
    void decide_activeCohortConflictWinsOverCandidateAuthoringErrors() {
        ChecklistTemplate legacy = makeTemplate(template -> {
            template.setStage(com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY);
            template.setEligibilityAnchorType(ChecklistAnchorType.NONE);
            template.setEligibilityRangeUnit(ChecklistRangeUnit.DAY);
            template.setEligibilityStartInclusive(0);
            template.setEligibilityEndInclusive(0);
            template.setStatus(ChecklistTemplateStatus.APPROVED);
            template.setDistributionEnabled(true);
            template.setSequencePosition(0);
        });
        ChecklistTemplate sequence = makeTemplate(template -> {
            template.setStage(com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY);
            template.setEligibilityAnchorType(ChecklistAnchorType.NONE);
            template.setEligibilityRangeUnit(ChecklistRangeUnit.DAY);
            template.setEligibilityStartInclusive(0);
            template.setEligibilityEndInclusive(0);
            template.setStatus(ChecklistTemplateStatus.PENDING_REVIEW);
            template.setSequencePosition(1);
        });
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(sequence));
        when(checklistTemplateRepository.findAllDistributionEnabledByStageAndStatus(
                com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY,
                ChecklistTemplateStatus.APPROVED)).thenReturn(List.of(legacy));
        ContentException ex = assertThrows(ContentException.class, () -> service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal));

        assertEquals(ChecklistTemplateApprovalServiceImpl.ACTIVE_LEGACY_CONFLICT,
                ex.getMetadata().get("reasonCode"));
        verify(checklistTemplateRepository, never()).save(any());
    }

    @Test
    void decide_legacyWithActiveSequence_rejectsBeforeMutationWithTypedReason() {
        ChecklistTemplate sequence = makeTemplate(template -> {
            template.setStage(com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY);
            template.setEligibilityAnchorType(ChecklistAnchorType.NONE);
            template.setEligibilityRangeUnit(ChecklistRangeUnit.DAY);
            template.setEligibilityStartInclusive(0);
            template.setEligibilityEndInclusive(0);
            template.setStatus(ChecklistTemplateStatus.APPROVED);
            template.setDistributionEnabled(true);
            template.setSequencePosition(1);
        });
        ChecklistTemplate legacy = makeTemplate(template -> {
            template.setStage(com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY);
            template.setEligibilityAnchorType(ChecklistAnchorType.NONE);
            template.setEligibilityRangeUnit(ChecklistRangeUnit.DAY);
            template.setEligibilityStartInclusive(0);
            template.setEligibilityEndInclusive(0);
            template.setStatus(ChecklistTemplateStatus.PENDING_REVIEW);
            template.setSequencePosition(0);
        });
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(legacy));
        when(checklistTemplateRepository.findAllDistributionEnabledByStageAndStatus(
                com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY,
                ChecklistTemplateStatus.APPROVED)).thenReturn(List.of(sequence));

        ContentException ex = assertThrows(ContentException.class, () -> service.decide(
                TEMPLATE_ID, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal));

        assertEquals(ChecklistTemplateApprovalServiceImpl.ACTIVE_SEQUENCE_CONFLICT,
                ex.getMetadata().get("reasonCode"));
        verify(checklistTemplateRepository, never()).save(any());
        verify(checklistAuditWriter, never()).write(any());
    }

    @Test
    void decide_positiveSequenceApprovesContiguousPositionsInOrder() {
        UUID positionOneId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID positionTwoId = UUID.fromString("00000000-0000-0000-0000-000000000102");
        UUID positionThreeId = UUID.fromString("00000000-0000-0000-0000-000000000103");
        ChecklistTemplate positionOne = positiveTemplate(positionOneId, 1);
        ChecklistTemplate positionTwo = positiveTemplate(positionTwoId, 2);
        ChecklistTemplate positionThree = positiveTemplate(positionThreeId, 3);

        when(checklistTemplateRepository.findById(positionOneId)).thenReturn(Optional.of(positionOne));
        when(checklistTemplateRepository.findById(positionTwoId)).thenReturn(Optional.of(positionTwo));
        when(checklistTemplateRepository.findById(positionThreeId)).thenReturn(Optional.of(positionThree));
        when(checklistTemplateRepository.findAllDistributionEnabledByStageAndStatus(
                com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY,
                ChecklistTemplateStatus.APPROVED))
                .thenReturn(List.of(), List.of(positionOne), List.of(positionOne, positionTwo));
        when(checklistItemRepository.findByTemplate_IdOrderByOrder(positionOneId))
                .thenReturn(List.of(ChecklistTemplateTestFactory.makeItem(positionOne, 1)));
        when(checklistItemRepository.findByTemplate_IdOrderByOrder(positionTwoId))
                .thenReturn(List.of(ChecklistTemplateTestFactory.makeItem(positionTwo, 1)));
        when(checklistItemRepository.findByTemplate_IdOrderByOrder(positionThreeId))
                .thenReturn(List.of(ChecklistTemplateTestFactory.makeItem(positionThree, 1)));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.decide(positionOneId, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal);
        service.decide(positionTwoId, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal);
        service.decide(positionThreeId, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal);

        assertEquals(ChecklistTemplateStatus.APPROVED, positionOne.getStatus());
        assertEquals(ChecklistTemplateStatus.APPROVED, positionTwo.getStatus());
        assertEquals(ChecklistTemplateStatus.APPROVED, positionThree.getStatus());
        assertEquals(Boolean.TRUE, positionOne.getDistributionEnabled());
        assertEquals(Boolean.TRUE, positionTwo.getDistributionEnabled());
        assertEquals(Boolean.TRUE, positionThree.getDistributionEnabled());
        verify(checklistTemplateRepository, org.mockito.Mockito.times(3)).save(any(ChecklistTemplate.class));
    }

    @Test
    void decide_positiveSequenceReplacesSameLineagePositionAfterValidation() {
        UUID lineageId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID activeId = UUID.fromString("00000000-0000-0000-0000-000000000202");
        UUID candidateId = UUID.fromString("00000000-0000-0000-0000-000000000203");
        ChecklistTemplate active = positiveTemplate(activeId, 1);
        active.setTemplateLineageId(lineageId);
        active.setStatus(ChecklistTemplateStatus.APPROVED);
        active.setDistributionEnabled(true);
        ChecklistTemplate candidate = positiveTemplate(candidateId, 1);
        candidate.setTemplateLineageId(lineageId);

        when(checklistTemplateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(checklistTemplateRepository.findAllDistributionEnabledByStageAndStatus(
                com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY,
                ChecklistTemplateStatus.APPROVED)).thenReturn(List.of(active));
        when(checklistItemRepository.findByTemplate_IdOrderByOrder(candidateId))
                .thenReturn(List.of(ChecklistTemplateTestFactory.makeItem(candidate, 1)));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.decide(candidateId, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal);

        assertEquals(ChecklistTemplateStatus.ARCHIVED, active.getStatus());
        assertEquals(Boolean.FALSE, active.getDistributionEnabled());
        assertEquals(ChecklistTemplateStatus.APPROVED, candidate.getStatus());
        verify(checklistTemplateRepository, org.mockito.Mockito.times(2)).save(any(ChecklistTemplate.class));
    }

    @Test
    void decide_replacementValidationFailureDoesNotArchiveExistingPosition() {
        UUID lineageId = UUID.fromString("00000000-0000-0000-0000-000000000301");
        UUID activeTwoId = UUID.fromString("00000000-0000-0000-0000-000000000302");
        UUID activeThreeId = UUID.fromString("00000000-0000-0000-0000-000000000303");
        UUID candidateId = UUID.fromString("00000000-0000-0000-0000-000000000304");
        ChecklistTemplate activeTwo = positiveTemplate(activeTwoId, 2);
        activeTwo.setTemplateLineageId(lineageId);
        activeTwo.setStatus(ChecklistTemplateStatus.APPROVED);
        activeTwo.setDistributionEnabled(true);
        ChecklistTemplate activeThree = positiveTemplate(activeThreeId, 3);
        activeThree.setTemplateLineageId(lineageId);
        activeThree.setStatus(ChecklistTemplateStatus.APPROVED);
        activeThree.setDistributionEnabled(true);
        ChecklistTemplate candidate = positiveTemplate(candidateId, 2);
        candidate.setTemplateLineageId(lineageId);

        when(checklistTemplateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(checklistTemplateRepository.findAllDistributionEnabledByStageAndStatus(
                com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY,
                ChecklistTemplateStatus.APPROVED)).thenReturn(List.of(activeTwo, activeThree));
        ContentException ex = assertThrows(ContentException.class, () -> service.decide(
                candidateId, new ContentDecisionRequest(ContentDecision.APPROVE, null), principal));

        assertEquals(ChecklistTemplateApprovalServiceImpl.SEQUENCE_POSITION_GAP,
                ex.getMetadata().get("reasonCode"));
        assertEquals(ChecklistTemplateStatus.APPROVED, activeTwo.getStatus());
        assertEquals(Boolean.TRUE, activeTwo.getDistributionEnabled());
        verify(checklistTemplateRepository, never()).save(any());
    }

    private static ChecklistTemplate positiveTemplate(UUID id, int position) {
        ChecklistTemplate template = makeTemplate(value -> {
            value.setId(id);
            value.setTemplateLineageId(id);
            value.setTemplateVersionId(id);
            value.setStage(com.carebridge.backend.content.entity.ContentStage.PRE_PREGNANCY);
            value.setEligibilityAnchorType(ChecklistAnchorType.NONE);
            value.setEligibilityRangeUnit(ChecklistRangeUnit.DAY);
            value.setEligibilityStartInclusive(0);
            value.setEligibilityEndInclusive(0);
            value.setStatus(ChecklistTemplateStatus.PENDING_REVIEW);
            value.setSequencePosition(position);
        });
        return template;
    }
}
