package com.carebridge.backend.checklist;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.dto.UpdateChecklistItemRequest;
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.checklist.service.impl.UserChecklistItemServiceImpl;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserChecklistItemSystemTaskMutationTest {

    @Mock private UserChecklistItemRepository checklistRepository;
    @Mock private ChecklistItemRepository templateItemRepository;
    @Mock private AuditService auditService;
    @Mock private LifecycleContentStageResolver lifecycleContentStageResolver;
    @Mock private BabyProfileRepository babyProfileRepository;

    private UserChecklistItemServiceImpl service;
    private UUID actorId;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        service = new UserChecklistItemServiceImpl(
                checklistRepository,
                templateItemRepository,
                auditService,
                lifecycleContentStageResolver,
                babyProfileRepository,
                new UnifiedTaskMutationPolicy());
        actorId = UUID.randomUUID();
        itemId = UUID.randomUUID();
    }

    @Test
    void updateSystemTaskRejectsEvenOrderOnlyMutation() {
        assertThatThrownBy(() -> service.updateItem(
                itemId, new UpdateChecklistItemRequest(null, null, 2), actorId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.getCode())
                            .isEqualTo("CHECKLIST_LEGACY_ROUTE_RETIRED");
                    org.assertj.core.api.Assertions.assertThat(exception.getHttpStatus().value())
                            .isEqualTo(410);
                });

        verifyNoInteractions(checklistRepository);
    }

    @Test
    void deleteSystemTaskRejectsBeforePersistenceMutation() {
        assertThatThrownBy(() -> service.deleteItem(itemId, actorId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.getCode())
                            .isEqualTo("CHECKLIST_LEGACY_ROUTE_RETIRED");
                    org.assertj.core.api.Assertions.assertThat(exception.getHttpStatus().value())
                            .isEqualTo(410);
                });

        verifyNoInteractions(checklistRepository);
    }
}
