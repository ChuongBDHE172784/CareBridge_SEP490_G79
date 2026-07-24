package com.carebridge.backend.checklist;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
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
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import com.carebridge.backend.content.policy.ResolvedLifecycleContext;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ITEM_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID TEMPLATE_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    private static final UUID CHECKLIST_TEMPLATE_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000002");
    private static final UUID JOURNEY_ID = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");
    private static final UUID OTHER_JOURNEY_ID = UUID.fromString("eeeeeeee-0000-0000-0000-000000000002");
    private static final UUID BABY_ID = UUID.fromString("ffffffff-0000-0000-0000-000000000001");

    @Mock private UserChecklistItemRepository checklistRepository;
    @Mock private ChecklistItemRepository templateItemRepository;
    @Mock private AuditService auditService;
    @Mock private LifecycleContentStageResolver lifecycleContentStageResolver;
    @Mock private BabyProfileRepository babyProfileRepository;
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

    private ChecklistItem makeSourceTemplateItem() {
        ChecklistTemplate template = ChecklistTemplate.builder()
                .id(CHECKLIST_TEMPLATE_ID)
                .stage(ContentStage.PRE_PREGNANCY)
                .status(ChecklistTemplateStatus.APPROVED)
                .build();
        return ChecklistItem.builder()
                .id(TEMPLATE_ID)
                .template(template)
                .itemText("Register at hospital")
                .order(1)
                .build();
    }

    private void stubApprovedJourneyImport(int inserted) {
        when(lifecycleContentStageResolver.resolveForUpdate(OWNER_ID))
                .thenReturn(new ResolvedLifecycleContext(JOURNEY_ID, ContentStage.PRE_PREGNANCY));
        when(templateItemRepository.findAllAvailableByIdInForUpdate(
                List.of(TEMPLATE_ID), ChecklistTemplateStatus.APPROVED, ContentStage.PRE_PREGNANCY))
                .thenReturn(List.of(makeSourceTemplateItem()));
        when(checklistRepository.insertImportedIfAbsent(
                any(), eq(OWNER_ID), eq(JOURNEY_ID), isNull(), eq(TEMPLATE_ID),
                eq("Register at hospital"), eq(1)))
                .thenReturn(inserted);
        when(checklistRepository.findImportedByExactScope(OWNER_ID, JOURNEY_ID, null, TEMPLATE_ID))
                .thenReturn(Optional.of(makeTemplateItem()));
    }

    @Test
    void addItem_customItem_returnsResponse() {
        when(checklistRepository.save(any())).thenReturn(makeCustomItem());

        var request = new AddChecklistItemRequest(null, null, "Pack hospital bag", ChecklistCategory.DELIVERY, 1);
        var response = service.addItem(request, OWNER_ID);

        assertThat(response.itemId()).isEqualTo(ITEM_ID);
        assertThat(response.itemText()).isEqualTo("Pack hospital bag");
        assertThat(response.completed()).isFalse();
    }

    @Test
    void importFromTemplate_returnsResponses() {
        stubApprovedJourneyImport(1);

        var result = service.importFromTemplate(
                new ImportFromTemplateRequest(JOURNEY_ID, null, List.of(TEMPLATE_ID)), OWNER_ID);

        assertThat(result).singleElement().extracting(response -> response.itemId()).isEqualTo(ITEM_ID);
        verify(auditService).log(
                eq(AuditAction.CHECKLIST_ITEM_ADDED), eq(OWNER_ID),
                eq("UserChecklistItem"), eq(ITEM_ID.toString()), eq("imported"));
    }

    @Test
    void listItems_returnsOwnerItems() {
        when(checklistRepository.findByOwnerFiltered(OWNER_ID, null, null))
                .thenReturn(List.of(makeCustomItem()));

        var result = service.listItems(OWNER_ID, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().ownerUserId()).isEqualTo(OWNER_ID);
    }

    @Test
    void listItems_empty_returnsEmptyList() {
        when(checklistRepository.findByOwnerFiltered(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertThat(service.listItems(OWNER_ID, null, null)).isEmpty();
    }

    @Test
    void toggleComplete_falseToTrue_returnsCompleted() {
        when(checklistRepository.findByIdAndOwnerUserId(ITEM_ID, OWNER_ID))
                .thenReturn(Optional.of(makeCustomItem()));
        when(checklistRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.toggleComplete(ITEM_ID, OWNER_ID).completed()).isTrue();
    }

    @Test
    void updateItem_templateItemTextChanged_throwsBusinessException() {
        when(checklistRepository.findByIdAndOwnerUserId(ITEM_ID, OWNER_ID))
                .thenReturn(Optional.of(makeTemplateItem()));

        var request = new UpdateChecklistItemRequest("Changed text", null, null);
        assertThatThrownBy(() -> service.updateItem(ITEM_ID, request, OWNER_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteItem_nonOwner_throwsResourceNotFound() {
        when(checklistRepository.findByIdAndOwnerUserId(ITEM_ID, OTHER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteItem(ITEM_ID, OTHER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void importFromTemplate_foreignJourney_rejectsBeforeAnyWrite() {
        when(lifecycleContentStageResolver.resolveForUpdate(OWNER_ID))
                .thenReturn(new ResolvedLifecycleContext(OTHER_JOURNEY_ID, ContentStage.PRE_PREGNANCY));

        assertThatThrownBy(() -> service.importFromTemplate(
                        new ImportFromTemplateRequest(JOURNEY_ID, null, List.of(TEMPLATE_ID)), OWNER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("CHECKLIST-007"));
        verifyNoInteractions(templateItemRepository, checklistRepository);
    }

    @Test
    void importFromTemplate_missingCanonicalJourneyWithExplicitId_isNeutralUnavailable() {
        when(lifecycleContentStageResolver.resolveForUpdate(OWNER_ID))
                .thenThrow(ContentException.lifecycleContextUnavailable());

        assertThatThrownBy(() -> service.importFromTemplate(
                        new ImportFromTemplateRequest(JOURNEY_ID, null, List.of(TEMPLATE_ID)), OWNER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("CHECKLIST-007"));
        verifyNoInteractions(templateItemRepository, checklistRepository);
    }

    @Test
    void importFromTemplate_unrelatedBaby_rejectsBeforeAnyWrite() {
        when(babyProfileRepository.findOwnedActiveByIdForUpdate(BABY_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importFromTemplate(
                        new ImportFromTemplateRequest(null, BABY_ID, List.of(TEMPLATE_ID)), OWNER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("CHECKLIST-007"));
        verifyNoInteractions(templateItemRepository, checklistRepository);
    }

    @Test
    void importFromTemplate_unapprovedSource_rejectsBeforeAnyWrite() {
        when(lifecycleContentStageResolver.resolveForUpdate(OWNER_ID))
                .thenReturn(new ResolvedLifecycleContext(JOURNEY_ID, ContentStage.PRE_PREGNANCY));
        when(templateItemRepository.findAllAvailableByIdInForUpdate(
                List.of(TEMPLATE_ID), ChecklistTemplateStatus.APPROVED, ContentStage.PRE_PREGNANCY))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.importFromTemplate(
                        new ImportFromTemplateRequest(JOURNEY_ID, null, List.of(TEMPLATE_ID)), OWNER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("CHECKLIST-007"));
        verifyNoInteractions(checklistRepository);
    }

    @Test
    void importFromTemplate_duplicateRequestIds_createsAndAuditsOnlyOnce() {
        stubApprovedJourneyImport(1);

        var result = service.importFromTemplate(
                new ImportFromTemplateRequest(JOURNEY_ID, null, List.of(TEMPLATE_ID, TEMPLATE_ID)), OWNER_ID);

        assertThat(result).hasSize(1);
        verify(checklistRepository, times(1)).insertImportedIfAbsent(
                any(), eq(OWNER_ID), eq(JOURNEY_ID), isNull(), eq(TEMPLATE_ID),
                eq("Register at hospital"), eq(1));
        verify(auditService, times(1)).log(
                eq(AuditAction.CHECKLIST_ITEM_ADDED), eq(OWNER_ID),
                eq("UserChecklistItem"), any(), eq("imported"));
    }

    @Test
    void importFromTemplate_retryReturnsExistingWithoutDuplicateAudit() {
        stubApprovedJourneyImport(0);

        var result = service.importFromTemplate(
                new ImportFromTemplateRequest(JOURNEY_ID, null, List.of(TEMPLATE_ID)), OWNER_ID);

        assertThat(result).singleElement().extracting(response -> response.itemId()).isEqualTo(ITEM_ID);
        verifyNoInteractions(auditService);
    }
}
