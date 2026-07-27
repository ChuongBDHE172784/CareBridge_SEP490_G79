package com.carebridge.backend.content;

import static com.carebridge.backend.content.ChecklistTemplateTestFactory.ADMIN_ID;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.TEMPLATE_ID;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.makeItem;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.makeTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.dto.request.ChecklistItemRequest;
import com.carebridge.backend.content.dto.request.CreateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.HideChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.UpdateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateDetailResponse;
import com.carebridge.backend.content.dto.response.HideChecklistTemplateResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.service.AdminChecklistTemplateServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminChecklistTemplateServiceImplTest {

    @Mock
    private ChecklistTemplateRepository checklistTemplateRepository;

    @Mock
    private ChecklistItemRepository checklistItemRepository;

    @Spy
    private ContentMapper contentMapper = new ContentMapper();

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminChecklistTemplateServiceImpl service;

    private CreateChecklistTemplateRequest makeCreateRequest(List<ChecklistItemRequest> items) {
        return new CreateChecklistTemplateRequest("Checklist khám thai tháng 3", "Mô tả", ContentStage.PREGNANCY, items);
    }

    // CHKTPL-TC-001
    @Test
    void create_validWithItems_returnsDraftWithTwoItems() {
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(inv -> { ChecklistTemplate t = inv.getArgument(0); t.setId(TEMPLATE_ID); return t; });
        when(checklistItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        CreateChecklistTemplateRequest request = makeCreateRequest(List.of(
                new ChecklistItemRequest("Siêu âm đo độ mờ da gáy", 1, true),
                new ChecklistItemRequest("Xét nghiệm Double test", 2, true)));

        AdminChecklistTemplateDetailResponse response = service.create(request, ADMIN_ID);

        assertEquals(ChecklistTemplateStatus.DRAFT, response.getStatus());
        assertEquals(2, response.getItems().size());
        verify(checklistItemRepository).saveAll(anyList());
        verify(auditService).log(eq(AuditAction.CHECKLIST_TEMPLATE_CREATED), eq(ADMIN_ID), eq("ChecklistTemplate"), any(), any());
    }

    // CHKTPL-TC-002
    @Test
    void create_emptyItems_returnsDraftWithNoItems() {
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(inv -> { ChecklistTemplate t = inv.getArgument(0); t.setId(TEMPLATE_ID); return t; });

        AdminChecklistTemplateDetailResponse response = service.create(makeCreateRequest(List.of()), ADMIN_ID);

        assertEquals(ChecklistTemplateStatus.DRAFT, response.getStatus());
        assertTrue(response.getItems().isEmpty());
        verify(checklistItemRepository, never()).saveAll(anyList());
    }

    // CHKTPL-TC-002b — null items behaves the same as empty (§11.2)
    @Test
    void create_nullItems_returnsDraftWithNoItems() {
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(inv -> { ChecklistTemplate t = inv.getArgument(0); t.setId(TEMPLATE_ID); return t; });

        AdminChecklistTemplateDetailResponse response = service.create(makeCreateRequest(null), ADMIN_ID);

        assertTrue(response.getItems().isEmpty());
    }

    // CHKTPL-TC-004 — regression guard for the immutable-list bug (Logic Issue L3)
    @Test
    void update_reconcilesExistingIdsAndSoftDeactivatesOmittedItems() {
        ChecklistTemplate template = makeTemplate();
        ChecklistItem old1 = makeItem(template, 1);
        old1.setIsActive(false);
        ChecklistItem old2 = makeItem(template, 2);
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistItemRepository.findAllByTemplateIdOrderByOrder(TEMPLATE_ID))
                .thenReturn(List.of(old1, old2));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checklistItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        UpdateChecklistTemplateRequest request = new UpdateChecklistTemplateRequest(
                "Tên mới", "Mô tả mới", ContentStage.PREGNANCY, ChecklistTemplateStatus.DRAFT,
                List.of(new ChecklistItemRequest(old1.getId(), "Mục 1", 1, true),
                        new ChecklistItemRequest("Mục 2", 2, false),
                        new ChecklistItemRequest("Mục 3", 3, true)));

        AdminChecklistTemplateDetailResponse response = service.update(TEMPLATE_ID, request, ADMIN_ID);

        verify(checklistItemRepository, never()).deleteAll(anyList());
        ArgumentCaptor<List<ChecklistItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(checklistItemRepository).saveAll(captor.capture());
        assertEquals(4, captor.getValue().size());
        assertEquals(old1.getId(), response.getItems().getFirst().getId());
        assertTrue(old1.getIsActive());
        assertEquals(false, old2.getIsActive());
        assertEquals(3, response.getItems().size());
        verify(auditService).log(eq(AuditAction.CHECKLIST_TEMPLATE_UPDATED), eq(ADMIN_ID), eq("ChecklistTemplate"), any(), any());
    }

    @Test
    void update_duplicateExistingItemId_rejectsRequest() {
        ChecklistTemplate template = makeTemplate();
        ChecklistItem existing = makeItem(template, 1);
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checklistItemRepository.findAllByTemplateIdOrderByOrder(TEMPLATE_ID))
                .thenReturn(List.of(existing));
        UpdateChecklistTemplateRequest request = new UpdateChecklistTemplateRequest(
                "Updated", "Updated", ContentStage.PREGNANCY, ChecklistTemplateStatus.DRAFT,
                List.of(
                        new ChecklistItemRequest(existing.getId(), "First", 1, true),
                        new ChecklistItemRequest(existing.getId(), "Duplicate", 2, true)));

        ContentException error = assertThrows(
                ContentException.class, () -> service.update(TEMPLATE_ID, request, ADMIN_ID));

        assertEquals("CHKTPL-009", error.getCode());
        verify(checklistItemRepository, never()).saveAll(anyList());
    }

    @Test
    void update_itemIdFromAnotherTemplate_rejectsRequest() {
        ChecklistTemplate template = makeTemplate();
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checklistItemRepository.findAllByTemplateIdOrderByOrder(TEMPLATE_ID))
                .thenReturn(List.of());
        UpdateChecklistTemplateRequest request = new UpdateChecklistTemplateRequest(
                "Updated", "Updated", ContentStage.PREGNANCY, ChecklistTemplateStatus.DRAFT,
                List.of(new ChecklistItemRequest(UUID.randomUUID(), "Foreign", 1, true)));

        ContentException error = assertThrows(
                ContentException.class, () -> service.update(TEMPLATE_ID, request, ADMIN_ID));

        assertEquals("CHKTPL-009", error.getCode());
        verify(checklistItemRepository, never()).saveAll(anyList());
    }

    // CHKTPL-TC-005
    @Test
    void update_nullItems_leavesExistingItemsUntouched() {
        ChecklistTemplate template = makeTemplate();
        ChecklistItem existing = makeItem(template, 1);
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistItemRepository.findByTemplate_IdOrderByOrder(TEMPLATE_ID)).thenReturn(List.of(existing));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateChecklistTemplateRequest request = new UpdateChecklistTemplateRequest(
                "Tên mới", "Mô tả mới", ContentStage.PREGNANCY, ChecklistTemplateStatus.DRAFT, null);

        AdminChecklistTemplateDetailResponse response = service.update(TEMPLATE_ID, request, ADMIN_ID);

        verify(checklistItemRepository, never()).deleteAll(anyList());
        verify(checklistItemRepository, never()).saveAll(anyList());
        assertEquals(1, response.getItems().size());
    }

    // update: current or target status outside DRAFT/PENDING_REVIEW -> CHKTPL-004
    @Test
    void update_currentStatusApproved_throwsChktpl004() {
        ChecklistTemplate template = makeTemplate(t -> t.setStatus(ChecklistTemplateStatus.APPROVED));
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        UpdateChecklistTemplateRequest request = new UpdateChecklistTemplateRequest(
                "Tên mới", "Mô tả", ContentStage.PREGNANCY, ChecklistTemplateStatus.DRAFT, null);

        ContentException ex = assertThrows(ContentException.class,
                () -> service.update(TEMPLATE_ID, request, ADMIN_ID));
        assertEquals("CHKTPL-004", ex.getCode());
        verify(checklistTemplateRepository, never()).save(any());
    }

    // CHKTPL-TC-006
    @Test
    void archive_validReason_transitionsToArchivedWithoutTouchingItems() {
        ChecklistTemplate template = makeTemplate();
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        HideChecklistTemplateResponse response = service.archive(
                TEMPLATE_ID, new HideChecklistTemplateRequest("Nội dung lỗi thời"), ADMIN_ID);

        assertEquals(ChecklistTemplateStatus.ARCHIVED, response.newStatus());
        verify(checklistItemRepository, never()).deleteAll(anyList());
        verify(auditService).log(eq(AuditAction.CHECKLIST_TEMPLATE_ARCHIVED), eq(ADMIN_ID), eq("ChecklistTemplate"), any(), any());
    }

    // CHKTPL-TC-007
    @Test
    void archive_blankReason_throwsChktpl005() {
        ChecklistTemplate template = makeTemplate();
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        ContentException ex = assertThrows(ContentException.class, () -> service.archive(
                TEMPLATE_ID, new HideChecklistTemplateRequest(""), ADMIN_ID));

        assertEquals("CHKTPL-005", ex.getCode());
        verify(checklistTemplateRepository, never()).save(any());
    }

    // CHKTPL-TC-008
    @Test
    void archive_alreadyArchived_throwsChktpl006() {
        ChecklistTemplate template = makeTemplate(t -> t.setStatus(ChecklistTemplateStatus.ARCHIVED));
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        ContentException ex = assertThrows(ContentException.class, () -> service.archive(
                TEMPLATE_ID, new HideChecklistTemplateRequest("Lý do"), ADMIN_ID));

        assertEquals("CHKTPL-006", ex.getCode());
        verify(checklistTemplateRepository, never()).save(any());
    }

    // CHKTPL-003 — not found guard (referenced by TDS §9, applies to getById/update/archive)
    @Test
    void getById_notFound_throwsChktpl003() {
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());

        ContentException ex = assertThrows(ContentException.class, () -> service.getById(TEMPLATE_ID));
        assertEquals("CHKTPL-003", ex.getCode());
    }

    @Test
    void update_resubmitReturnedDraft_clearsLatestReviewFeedback() {
        ChecklistTemplate template = makeTemplate();
        template.setRevisionReason("Bổ sung mục bắt buộc");
        template.setRevisionRequestedAt(Instant.now());
        template.setRevisionRequestedBy(ADMIN_ID);
        template.setRevisionRequestedVersion(1);
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checklistItemRepository.findByTemplate_IdOrderByOrder(TEMPLATE_ID)).thenReturn(List.of());

        service.update(TEMPLATE_ID, new UpdateChecklistTemplateRequest(
                template.getName(), template.getDescription(), template.getStage(),
                ChecklistTemplateStatus.PENDING_REVIEW, null), ADMIN_ID);

        assertEquals(ChecklistTemplateStatus.PENDING_REVIEW, template.getStatus());
        assertEquals(null, template.getRevisionReason());
        assertEquals(null, template.getRevisionRequestedAt());
        assertEquals(null, template.getRevisionRequestedBy());
        assertEquals(null, template.getRevisionRequestedVersion());
    }

    @Test
    void update_saveReturnedDraft_retainsLatestReviewFeedback() {
        ChecklistTemplate template = makeTemplate();
        Instant requestedAt = Instant.now();
        template.setRevisionReason("Bổ sung mục bắt buộc");
        template.setRevisionRequestedAt(requestedAt);
        template.setRevisionRequestedBy(ADMIN_ID);
        template.setRevisionRequestedVersion(1);
        when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checklistItemRepository.findByTemplate_IdOrderByOrder(TEMPLATE_ID)).thenReturn(List.of());

        service.update(TEMPLATE_ID, new UpdateChecklistTemplateRequest(
                template.getName(), template.getDescription(), template.getStage(),
                ChecklistTemplateStatus.DRAFT, null), ADMIN_ID);

        assertEquals("Bổ sung mục bắt buộc", template.getRevisionReason());
        assertEquals(requestedAt, template.getRevisionRequestedAt());
        assertEquals(ADMIN_ID, template.getRevisionRequestedBy());
        assertEquals(1, template.getRevisionRequestedVersion());
    }
}
