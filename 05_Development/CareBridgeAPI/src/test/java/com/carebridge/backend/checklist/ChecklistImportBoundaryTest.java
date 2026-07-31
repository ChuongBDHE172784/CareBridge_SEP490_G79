package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.dto.ImportFromTemplateRequest;
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.checklist.service.impl.UserChecklistItemServiceImpl;
import com.carebridge.backend.checklist.today.policy.UnifiedTaskMutationPolicy;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Boundary proof for the retired import adapter after CHK-025 cutover. */
@ExtendWith(MockitoExtension.class)
class ChecklistImportBoundaryTest {
    @Mock private UserChecklistItemRepository legacyRepository;
    @Mock private ChecklistItemRepository templateRepository;
    @Mock private AuditService auditService;
    @Mock private LifecycleContentStageResolver lifecycleResolver;
    @Mock private BabyProfileRepository babyRepository;
    private UserChecklistItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserChecklistItemServiceImpl(legacyRepository, templateRepository, auditService,
                lifecycleResolver, babyRepository, new UnifiedTaskMutationPolicy());
    }

    @Test
    void everyPreviouslyValidImportShapeIsGoneBeforeLookupOrWrite() {
        UUID owner = UUID.randomUUID();
        for (ImportFromTemplateRequest request : List.of(
                new ImportFromTemplateRequest(UUID.randomUUID(), null, List.of(UUID.randomUUID())),
                new ImportFromTemplateRequest(null, UUID.randomUUID(), List.of(UUID.randomUUID())),
                new ImportFromTemplateRequest(null, null, List.of(UUID.randomUUID())))) {
            assertThatThrownBy(() -> service.importFromTemplate(request, owner))
                    .isInstanceOfSatisfying(BusinessException.class, error -> {
                        assertThat(error.getHttpStatus().value()).isEqualTo(410);
                        assertThat(error.getCode()).isEqualTo("CHECKLIST_LEGACY_ROUTE_RETIRED");
                    });
        }
        verifyNoInteractions(legacyRepository, templateRepository, auditService,
                lifecycleResolver, babyRepository);
    }
}
