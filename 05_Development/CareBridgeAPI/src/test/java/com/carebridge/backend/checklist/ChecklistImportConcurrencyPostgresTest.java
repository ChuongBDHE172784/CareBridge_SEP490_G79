package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Concurrent callers cannot revive the retired legacy import writer. */
@ExtendWith(MockitoExtension.class)
class ChecklistImportConcurrencyPostgresTest {
    @Mock private UserChecklistItemRepository legacyRepository;
    @Mock private ChecklistItemRepository templateRepository;
    @Mock private AuditService auditService;
    @Mock private LifecycleContentStageResolver lifecycleResolver;
    @Mock private BabyProfileRepository babyRepository;

    @Test
    void concurrentLegacyImportsBothFailClosedBeforePersistence() throws Exception {
        var service = new UserChecklistItemServiceImpl(legacyRepository, templateRepository, auditService,
                lifecycleResolver, babyRepository, new UnifiedTaskMutationPolicy());
        var request = new ImportFromTemplateRequest(
                UUID.randomUUID(), null, List.of(UUID.randomUUID()));
        UUID actor = UUID.randomUUID();
        Callable<String> invocation = () -> {
            try {
                service.importFromTemplate(request, actor);
                return "unexpected-success";
            } catch (BusinessException error) {
                return error.getHttpStatus().value() + ":" + error.getCode();
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(invocation, invocation)).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception error) {
                            throw new AssertionError(error);
                        }
                    }).toList();
            assertThat(results).containsExactlyInAnyOrder(
                    "410:CHECKLIST_LEGACY_ROUTE_RETIRED",
                    "410:CHECKLIST_LEGACY_ROUTE_RETIRED");
        }
        verifyNoInteractions(legacyRepository, templateRepository, auditService,
                lifecycleResolver, babyRepository);
    }
}
