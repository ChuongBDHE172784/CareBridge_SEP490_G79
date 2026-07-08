package com.carebridge.backend.checklist;

import com.carebridge.backend.audit.service.AuditService;
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
import com.carebridge.backend.content.repository.ChecklistItemRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {

    static final UUID OWNER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OTHER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID ITEM_ID     = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    static final UUID TEMPLATE_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

    @Mock private UserChecklistItemRepository checklistRepository;
    @Mock private ChecklistItemRepository templateItemRepository;
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
                .templateItemId(TEMPLATE_ID)
                .itemText("Register at hospital")
                .category(ChecklistCategory.PAPERWORK)
                .completed(false)
                .itemOrder(2)
                .createdAt(Instant.now())
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
        var templateItem = ChecklistItem.builder()
                .id(TEMPLATE_ID)
                .itemText("Register at hospital")
                .order(1)
                .build();
        when(templateItemRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(templateItem));
        when(checklistRepository.save(any())).thenReturn(makeTemplateItem());

        var request = new ImportFromTemplateRequest(null, null, List.of(TEMPLATE_ID));
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
}
