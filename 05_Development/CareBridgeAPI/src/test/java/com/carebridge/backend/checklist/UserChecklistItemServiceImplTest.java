package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.entity.UserChecklistItem;
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.checklist.service.impl.UserChecklistItemServiceImpl;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserChecklistItemServiceImplTest {

    @Test
    void listItemsOmitsImportedItemWhoseTemplateIsArchived() {
        UUID owner = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        ChecklistTemplate archived = ChecklistTemplate.builder()
                .id(UUID.randomUUID())
                .status(ChecklistTemplateStatus.ARCHIVED)
                .build();
        ChecklistItem templateItem = ChecklistItem.builder()
                .id(itemId)
                .template(archived)
                .itemText("Archived item")
                .build();
        UserChecklistItem imported = UserChecklistItem.builder()
                .id(UUID.randomUUID())
                .ownerUserId(owner)
                .templateItemId(itemId)
                .itemText("Archived item")
                .build();
        UserChecklistItem userCreated = UserChecklistItem.builder()
                .id(UUID.randomUUID())
                .ownerUserId(owner)
                .itemText("Personal item")
                .build();
        UserChecklistItemRepository checklist = mock(UserChecklistItemRepository.class);
        ChecklistItemRepository templateItems = mock(ChecklistItemRepository.class);
        AuditService audit = mock(AuditService.class);
        LifecycleContentStageResolver stages = mock(LifecycleContentStageResolver.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        UnifiedTaskMutationPolicy mutationPolicy = mock(UnifiedTaskMutationPolicy.class);
        when(checklist.findByOwnerFiltered(owner, null, null))
                .thenReturn(List.of(imported, userCreated));
        when(templateItems.findAllWithTemplateByIdIn(List.of(itemId))).thenReturn(List.of(templateItem));

        UserChecklistItemServiceImpl service = new UserChecklistItemServiceImpl(
                checklist, templateItems, audit, stages, babies, mutationPolicy);

        assertThat(service.listItems(owner, null, null))
                .extracting(response -> response.itemText())
                .containsExactly("Personal item");
    }
}
