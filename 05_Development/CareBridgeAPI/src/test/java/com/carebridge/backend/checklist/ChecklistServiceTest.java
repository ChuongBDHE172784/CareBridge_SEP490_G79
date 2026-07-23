package com.carebridge.backend.checklist;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.dto.AddChecklistItemRequest;
import com.carebridge.backend.checklist.dto.ImportFromTemplateRequest;
import com.carebridge.backend.checklist.dto.UpdateChecklistItemRequest;
import com.carebridge.backend.checklist.entity.ChecklistCategory;
import com.carebridge.backend.checklist.entity.UserChecklistItem;
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.checklist.service.impl.UserChecklistItemServiceImpl;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {

    static final UUID OWNER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OTHER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID ITEM_ID     = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    static final UUID TEMPLATE_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    static final UUID CHECKLIST_TEMPLATE_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000002");
    static final UUID JOURNEY_ID  = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");
    static final UUID BABY_ID     = UUID.fromString("ffffffff-0000-0000-0000-000000000001");

    @Mock private UserChecklistItemRepository checklistRepository;
    @Mock private ChecklistItemRepository templateItemRepository;
    @Mock private ChecklistTemplateRepository templateRepository;
    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private BabyProfileRepository babyRepository;
    @Mock private AuditService auditService;
    @InjectMocks private UserChecklistItemServiceImpl service;

    private UserChecklistItem makeCustomItem() {
        return UserChecklistItem.builder()
                .id(ITEM_ID)
                .ownerUserId(OWNER_ID)
                .itemText("Pack hospital bag")
                .category(ChecklistCategory.DELIVERY)
                .completed(false)
                .itemOrder(1)
                .createdAt(Instant.now())
                .build();
    }

    private UserChecklistItem makeTemplateItem() {
        return UserChecklistItem.builder()
                .id(ITEM_ID)
                .ownerUserId(OWNER_ID)
                .journeyId(JOURNEY_ID)
                .templateItemId(TEMPLATE_ID)
                .itemText("Register at hospital")
                .category(ChecklistCategory.PAPERWORK)
                .completed(false)
                .itemOrder(2)
                .createdAt(Instant.now())
                .build();
    }

    private MotherJourney makeJourney(UUID ownerId, JourneyStatus status) {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(ownerId)
                .status(status)
                .build();
    }

    private ChecklistItem makeSourceTemplateItem(ContentStatus status) {
        ChecklistTemplate template = ChecklistTemplate.builder()
                .id(CHECKLIST_TEMPLATE_ID)
                .status(status)
                .build();
        return ChecklistItem.builder()
                .id(TEMPLATE_ID)
                .template(template)
                .itemText("Register at hospital")
                .order(1)
                .build();
    }

    // CHECKLIST-TC-001: addItem — custom item → returns ChecklistItemResponse
    @Test
    void addItem_customItem_returnsResponse() {
        when(checklistRepository.save(any())).thenReturn(makeCustomItem());

        var request = new AddChecklistItemRequest(null, null, "Pack hospital bag", ChecklistCategory.DELIVERY, 1);
        var response = service.addItem(request, OWNER_ID);

        assertThat(response).isNotNull();
        assertThat(response.itemId()).isEqualTo(ITEM_ID);
        assertThat(response.itemText()).isEqualTo("Pack hospital bag");
        assertThat(response.completed()).isFalse();
    }

    // CHECKLIST-TC-002: importFromTemplate → returns list of imported items
    @Test
    void importFromTemplate_returnsResponses() {
        var templateItem = makeSourceTemplateItem(ContentStatus.APPROVED);
        when(journeyRepository.findByIdAndOwnerUserIdAndStatus(
                JOURNEY_ID, OWNER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(makeJourney(OWNER_ID, JourneyStatus.ACTIVE)));
        when(templateItemRepository.findByIdAndTemplate_Status(TEMPLATE_ID, ContentStatus.APPROVED))
                .thenReturn(Optional.of(templateItem));
        when(templateRepository.findByIdAndStatus(CHECKLIST_TEMPLATE_ID, ContentStatus.APPROVED))
                .thenReturn(Optional.of(templateItem.getTemplate()));
        when(checklistRepository.insertImportedIfAbsent(
                any(), eq(OWNER_ID), eq(JOURNEY_ID), isNull(), eq(TEMPLATE_ID),
                eq("Register at hospital"), eq(1)))
                .thenReturn(1);
        when(checklistRepository.findImportedByExactScope(
                OWNER_ID, JOURNEY_ID, null, TEMPLATE_ID))
                .thenReturn(Optional.of(makeTemplateItem()));

        var request = new ImportFromTemplateRequest(JOURNEY_ID, null, List.of(TEMPLATE_ID));
        var result = service.importFromTemplate(request, OWNER_ID);

        assertThat(result).isNotEmpty();
    }

    // CHECKLIST-TC-003: listItems → returns owner's items
    @Test
    void listItems_returnsOwnerItems() {
        when(checklistRepository.findByOwnerFiltered(OWNER_ID, null, null))
                .thenReturn(List.of(makeCustomItem()));

        var result = service.listItems(OWNER_ID, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ownerUserId()).isEqualTo(OWNER_ID);
    }

    // CHECKLIST-TC-004: listItems — empty → returns empty list
    @Test
    void listItems_empty_returnsEmptyList() {
        when(checklistRepository.findByOwnerFiltered(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        var result = service.listItems(OWNER_ID, null, null);

        assertThat(result).isEmpty();
    }

    // CHECKLIST-TC-005: toggleComplete → flips is_completed to true
    @Test
    void toggleComplete_falseToTrue_returnsCompleted() {
        when(checklistRepository.findByIdAndOwnerUserId(ITEM_ID, OWNER_ID))
                .thenReturn(Optional.of(makeCustomItem()));
        when(checklistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.toggleComplete(ITEM_ID, OWNER_ID);

        assertThat(response.completed()).isTrue();
    }

    // CHECKLIST-TC-006: updateItem — template item text is immutable → CHECKLIST-006
    @Test
    void updateItem_templateItemTextChanged_throwsBusinessException() {
        when(checklistRepository.findByIdAndOwnerUserId(ITEM_ID, OWNER_ID))
                .thenReturn(Optional.of(makeTemplateItem()));

        var request = new UpdateChecklistItemRequest("Changed text", null, null);
        assertThatThrownBy(() -> service.updateItem(ITEM_ID, request, OWNER_ID))
                .isInstanceOf(BusinessException.class);
    }

    // CHECKLIST-TC-007: deleteItem — non-owner → ResourceNotFoundException
    @Test
    void deleteItem_nonOwner_throwsResourceNotFound() {
        when(checklistRepository.findByIdAndOwnerUserId(ITEM_ID, OTHER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteItem(ITEM_ID, OTHER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void importFromTemplate_foreignJourney_rejectsBeforeAnyWrite() {
        when(journeyRepository.findByIdAndOwnerUserIdAndStatus(
                JOURNEY_ID, OWNER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.empty());

        var request = new ImportFromTemplateRequest(JOURNEY_ID, null, List.of(TEMPLATE_ID));

        assertThatThrownBy(() -> service.importFromTemplate(request, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(templateItemRepository);
        verifyNoInteractions(checklistRepository);
    }

    @Test
    void importFromTemplate_inactiveJourney_rejectsBeforeAnyWrite() {
        when(journeyRepository.findByIdAndOwnerUserIdAndStatus(
                JOURNEY_ID, OWNER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.empty());

        var request = new ImportFromTemplateRequest(JOURNEY_ID, null, List.of(TEMPLATE_ID));

        assertThatThrownBy(() -> service.importFromTemplate(request, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(templateItemRepository);
        verifyNoInteractions(checklistRepository);
    }

    @Test
    void importFromTemplate_unrelatedBaby_rejectsBeforeAnyWrite() {
        when(journeyRepository.findByIdAndOwnerUserIdAndStatus(
                JOURNEY_ID, OWNER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(makeJourney(OWNER_ID, JourneyStatus.ACTIVE)));
        when(babyRepository.findByIdAndOwnerUserIdAndRelatedJourneyIdAndStatusAndActiveTrue(
                BABY_ID, OWNER_ID, JOURNEY_ID, BabyProfileStatus.ACTIVE))
                .thenReturn(Optional.empty());

        var request = new ImportFromTemplateRequest(JOURNEY_ID, BABY_ID, List.of(TEMPLATE_ID));

        assertThatThrownBy(() -> service.importFromTemplate(request, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(templateItemRepository);
        verifyNoInteractions(checklistRepository);
    }

    @Test
    void importFromTemplate_unapprovedSource_rejectsBeforeAnyWrite() {
        when(journeyRepository.findByIdAndOwnerUserIdAndStatus(
                JOURNEY_ID, OWNER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(makeJourney(OWNER_ID, JourneyStatus.ACTIVE)));
        when(templateItemRepository.findByIdAndTemplate_Status(TEMPLATE_ID, ContentStatus.APPROVED))
                .thenReturn(Optional.empty());

        var request = new ImportFromTemplateRequest(JOURNEY_ID, null, List.of(TEMPLATE_ID));

        assertThatThrownBy(() -> service.importFromTemplate(request, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(checklistRepository);
    }

    @Test
    void importFromTemplate_duplicateRequestIds_createsAndAuditsOnlyOnce() {
        when(journeyRepository.findByIdAndOwnerUserIdAndStatus(
                JOURNEY_ID, OWNER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(makeJourney(OWNER_ID, JourneyStatus.ACTIVE)));
        when(templateItemRepository.findByIdAndTemplate_Status(TEMPLATE_ID, ContentStatus.APPROVED))
                .thenReturn(Optional.of(makeSourceTemplateItem(ContentStatus.APPROVED)));
        when(templateRepository.findByIdAndStatus(CHECKLIST_TEMPLATE_ID, ContentStatus.APPROVED))
                .thenReturn(Optional.of(ChecklistTemplate.builder()
                        .id(CHECKLIST_TEMPLATE_ID)
                        .status(ContentStatus.APPROVED)
                        .build()));
        when(checklistRepository.insertImportedIfAbsent(
                any(), eq(OWNER_ID), eq(JOURNEY_ID), isNull(), eq(TEMPLATE_ID),
                eq("Register at hospital"), eq(1)))
                .thenReturn(1);
        when(checklistRepository.findImportedByExactScope(
                OWNER_ID, JOURNEY_ID, null, TEMPLATE_ID))
                .thenReturn(Optional.of(makeTemplateItem()));

        var request = new ImportFromTemplateRequest(
                JOURNEY_ID, null, List.of(TEMPLATE_ID, TEMPLATE_ID));

        var result = service.importFromTemplate(request, OWNER_ID);

        assertThat(result).hasSize(1);
        verify(checklistRepository, times(1)).insertImportedIfAbsent(
                any(), eq(OWNER_ID), eq(JOURNEY_ID), isNull(), eq(TEMPLATE_ID),
                eq("Register at hospital"), eq(1));
        verify(auditService, times(1)).log(
                eq(com.carebridge.backend.audit.entity.AuditAction.CHECKLIST_ITEM_ADDED),
                eq(OWNER_ID), eq("UserChecklistItem"), any(), eq("imported"));
    }

    @Test
    void importFromTemplate_retryReturnsExistingWithoutDuplicateAudit() {
        when(journeyRepository.findByIdAndOwnerUserIdAndStatus(
                JOURNEY_ID, OWNER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(makeJourney(OWNER_ID, JourneyStatus.ACTIVE)));
        when(templateItemRepository.findByIdAndTemplate_Status(TEMPLATE_ID, ContentStatus.APPROVED))
                .thenReturn(Optional.of(makeSourceTemplateItem(ContentStatus.APPROVED)));
        when(templateRepository.findByIdAndStatus(CHECKLIST_TEMPLATE_ID, ContentStatus.APPROVED))
                .thenReturn(Optional.of(ChecklistTemplate.builder()
                        .id(CHECKLIST_TEMPLATE_ID)
                        .status(ContentStatus.APPROVED)
                        .build()));
        when(checklistRepository.insertImportedIfAbsent(
                any(), eq(OWNER_ID), eq(JOURNEY_ID), isNull(), eq(TEMPLATE_ID),
                eq("Register at hospital"), eq(1)))
                .thenReturn(0);
        when(checklistRepository.findImportedByExactScope(
                OWNER_ID, JOURNEY_ID, null, TEMPLATE_ID))
                .thenReturn(Optional.of(makeTemplateItem()));

        var result = service.importFromTemplate(
                new ImportFromTemplateRequest(JOURNEY_ID, null, List.of(TEMPLATE_ID)),
                OWNER_ID);

        assertThat(result).singleElement()
                .extracting(response -> response.itemId())
                .isEqualTo(ITEM_ID);
        verifyNoInteractions(auditService);
    }
}
