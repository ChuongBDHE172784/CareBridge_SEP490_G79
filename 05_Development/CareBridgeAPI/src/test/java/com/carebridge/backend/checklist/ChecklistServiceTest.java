package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.dto.AddChecklistItemRequest;
import com.carebridge.backend.checklist.dto.ImportFromTemplateRequest;
import com.carebridge.backend.checklist.dto.UpdateChecklistItemRequest;
import com.carebridge.backend.checklist.entity.ChecklistCategory;
import com.carebridge.backend.checklist.entity.UserChecklistItem;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.checklist.service.impl.UserChecklistItemServiceImpl;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();

    @Mock private UserChecklistItemRepository checklistRepository;
    @Mock private ChecklistItemRepository templateItemRepository;
    @Mock private AuditService auditService;
    @Mock private LifecycleContentStageResolver lifecycleContentStageResolver;
    @Mock private BabyProfileRepository babyProfileRepository;
    @Spy private UnifiedTaskMutationPolicy mutationPolicy = new UnifiedTaskMutationPolicy();
    @InjectMocks private UserChecklistItemServiceImpl service;

    @Test
    void everyLegacyMutationFailsClosedBeforePersistence() {
        AddChecklistItemRequest add = new AddChecklistItemRequest(
                UUID.randomUUID(), null, "Task", ChecklistCategory.GENERAL, 0,
                ChecklistTargetSubject.MOTHER, UUID.randomUUID());
        assertRetired(() -> service.addItem(add, OWNER_ID));
        assertRetired(() -> service.importFromTemplate(
                new ImportFromTemplateRequest(UUID.randomUUID(), null, List.of(UUID.randomUUID())), OWNER_ID));
        assertRetired(() -> service.toggleComplete(ITEM_ID, OWNER_ID));
        assertRetired(() -> service.updateItem(
                ITEM_ID, new UpdateChecklistItemRequest("Changed", null, null), OWNER_ID));
        assertRetired(() -> service.deleteItem(ITEM_ID, OWNER_ID));
        verifyNoInteractions(checklistRepository, templateItemRepository, auditService,
                lifecycleContentStageResolver, babyProfileRepository);
    }

    @Test
    void legacyReadRemainsReadOnlyDuringCutover() {
        UserChecklistItem row = UserChecklistItem.builder().id(ITEM_ID).ownerUserId(OWNER_ID)
                .itemText("Historical snapshot").category(ChecklistCategory.GENERAL)
                .itemOrder(1).createdAt(Instant.parse("2026-07-30T00:00:00Z")).build();
        when(checklistRepository.findByOwnerFiltered(OWNER_ID, null, null)).thenReturn(List.of(row));

        assertThat(service.listItems(OWNER_ID, null, null)).singleElement()
                .satisfies(item -> assertThat(item.itemId()).isEqualTo(ITEM_ID));
    }

    private static void assertRetired(org.assertj.core.api.ThrowableAssert.ThrowingCallable invocation) {
        assertThatThrownBy(invocation).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getHttpStatus().value()).isEqualTo(410);
            assertThat(error.getCode()).isEqualTo("CHECKLIST_LEGACY_ROUTE_RETIRED");
        });
    }
}
