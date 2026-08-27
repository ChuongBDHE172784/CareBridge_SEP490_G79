package com.carebridge.backend.checklist.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.distribution.ChecklistHistoryReconciliationService;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.history.service.ChecklistHistoryService;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

class ChecklistHistoryServiceTest {

    private static final UUID OWNER = UUID.fromString("31000000-0000-0000-0000-000000000001");
    private static final UUID ARCHIVED_VERSION = UUID.fromString("31000000-0000-0000-0000-000000000002");
    private static final UUID ACTIVE_VERSION = UUID.fromString("31000000-0000-0000-0000-000000000003");

    @Test
    void archivedTemplateRowsAreOmittedAndVisiblePageTotalsRemainAccurate() {
        ChecklistInstance archived = instance(UUID.fromString("31000000-0000-0000-0000-000000000004"),
                ARCHIVED_VERSION);
        ChecklistInstance active = instance(UUID.fromString("31000000-0000-0000-0000-000000000005"),
                ACTIVE_VERSION);
        ChecklistHistoryReconciliationService reconciliation = mock(ChecklistHistoryReconciliationService.class);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistTemplateRepository templates = mock(ChecklistTemplateRepository.class);
        MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
        BabyProfileRepository babies = mock(BabyProfileRepository.class);
        Page<ChecklistInstance> page = mock(Page.class);
        when(page.getContent()).thenReturn(List.of(archived, active));
        when(page.getNumber()).thenReturn(0);
        when(page.getSize()).thenReturn(20);
        when(page.getTotalElements()).thenReturn(1L);
        when(page.getTotalPages()).thenReturn(1);
        when(instances.findOwnerHistory(any(), any(), any())).thenReturn(page);
        when(templates.findAllByTemplateVersionIdIn(anyCollection()))
                .thenReturn(List.of(
                        ChecklistTemplate.builder().templateVersionId(ARCHIVED_VERSION)
                                .status(ChecklistTemplateStatus.ARCHIVED).build(),
                        ChecklistTemplate.builder().templateVersionId(ACTIVE_VERSION)
                                .status(ChecklistTemplateStatus.APPROVED).build()));
        when(tasks.findAllByChecklistInstanceIds(List.of(active.getId()))).thenReturn(List.of());

        ChecklistHistoryService service = new ChecklistHistoryService(
                reconciliation, instances, tasks, templates, journeys, babies,
                Clock.fixed(Instant.parse("2026-08-03T00:00:00Z"), ZoneOffset.UTC));

        var response = service.listHistory(OWNER, null, 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().checklistInstanceId()).isEqualTo(active.getId());
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        verify(reconciliation, never()).reconcile(any(), any(), any(), any());
    }

    private static ChecklistInstance instance(UUID id, UUID versionId) {
        return ChecklistInstance.builder()
                .id(id)
                .templateVersionId(versionId)
                .recipientUserId(OWNER)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .contextOwnerUserId(OWNER)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .historicalAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build();
    }
}
