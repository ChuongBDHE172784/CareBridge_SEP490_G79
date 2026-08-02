package com.carebridge.backend.checklist.sequence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.checklist.repository.ChecklistTaskInstanceRepository;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChecklistSequenceResolverTest {

    private static final UUID OWNER = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID INSTANCE = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID VERSION_1 = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID VERSION_2 = UUID.fromString("20000000-0000-0000-0000-000000000004");

    @Test
    void requiredCompletionExposesReadyWithoutMaterializingSuccessor() {
        ChecklistTemplateRepository templates = mock(ChecklistTemplateRepository.class);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistTemplate first = template(VERSION_1, 1, "Set 1");
        ChecklistTemplate second = template(VERSION_2, 2, "Set 2");
        when(templates.findAllDistributionEnabledByStageAndStatus(
                ContentStage.PRE_PREGNANCY, ChecklistTemplateStatus.APPROVED))
                .thenReturn(List.of(first, second));
        when(templates.findByStageAndStatusOrderByUpdatedAtDesc(
                ContentStage.PRE_PREGNANCY, ChecklistTemplateStatus.ARCHIVED))
                .thenReturn(List.of());
        when(templates.findByTemplateVersionId(VERSION_1)).thenReturn(Optional.of(first));
        when(templates.findByTemplateVersionId(VERSION_2)).thenReturn(Optional.of(second));
        ChecklistInstance current = ChecklistInstance.builder()
                .id(INSTANCE)
                .templateVersionId(VERSION_1)
                .recipientUserId(OWNER)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(UUID.fromString("20000000-0000-0000-0000-000000000005"))
                .contextOwnerUserId(OWNER)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .build();
        when(instances.findByRecipientUserIdAndHistoricalAtIsNull(OWNER)).thenReturn(List.of(current));
        when(tasks.findByChecklistInstanceIdOrderByDisplayOrder(INSTANCE)).thenReturn(List.of(
                ChecklistTaskInstance.builder()
                        .id(UUID.fromString("20000000-0000-0000-0000-000000000006"))
                        .checklistInstanceId(INSTANCE)
                        .required(true)
                        .status(ChecklistTaskStatus.COMPLETED)
                        .build()));

        var projection = new ChecklistSequenceResolver(templates, instances, tasks).resolve(OWNER);

        assertThat(projection.sequenceState()).isEqualTo(ChecklistSequenceState.READY_TO_ADVANCE);
        assertThat(projection.advanceAvailable()).isTrue();
        assertThat(projection.currentInstanceId()).isEqualTo(INSTANCE);
        assertThat(projection.nextSet().position()).isEqualTo(2);
    }

    @Test
    void optionalIncompleteTaskDoesNotBlockRequiredQualification() {
        ChecklistTemplateRepository templates = mock(ChecklistTemplateRepository.class);
        ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
        ChecklistTaskInstanceRepository tasks = mock(ChecklistTaskInstanceRepository.class);
        ChecklistTemplate first = template(VERSION_1, 1, "Set 1");
        when(templates.findAllDistributionEnabledByStageAndStatus(
                ContentStage.PRE_PREGNANCY, ChecklistTemplateStatus.APPROVED))
                .thenReturn(List.of(first));
        when(templates.findByStageAndStatusOrderByUpdatedAtDesc(
                ContentStage.PRE_PREGNANCY, ChecklistTemplateStatus.ARCHIVED))
                .thenReturn(List.of());
        when(templates.findByTemplateVersionId(VERSION_1)).thenReturn(Optional.of(first));
        ChecklistInstance current = ChecklistInstance.builder()
                .id(INSTANCE).templateVersionId(VERSION_1).recipientUserId(OWNER)
                .recipientRole(ChecklistRecipientRole.MOTHER).careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(UUID.fromString("20000000-0000-0000-0000-000000000005"))
                .contextOwnerUserId(OWNER).origin(ChecklistOrigin.SYSTEM_TEMPLATE).build();
        when(instances.findByRecipientUserIdAndHistoricalAtIsNull(OWNER)).thenReturn(List.of(current));
        when(tasks.findByChecklistInstanceIdOrderByDisplayOrder(INSTANCE)).thenReturn(List.of(
                ChecklistTaskInstance.builder().id(UUID.randomUUID()).checklistInstanceId(INSTANCE)
                        .required(true).status(ChecklistTaskStatus.COMPLETED).build(),
                ChecklistTaskInstance.builder().id(UUID.randomUUID()).checklistInstanceId(INSTANCE)
                        .required(false).status(ChecklistTaskStatus.PENDING).build()));

        var projection = new ChecklistSequenceResolver(templates, instances, tasks).resolve(OWNER);

        assertThat(projection.sequenceState()).isEqualTo(ChecklistSequenceState.SEQUENCE_COMPLETE);
        assertThat(projection.sequenceComplete()).isTrue();
    }

    private static ChecklistTemplate template(UUID version, int position, String name) {
        return ChecklistTemplate.builder().id(UUID.randomUUID()).templateVersionId(version)
                .templateLineageId(UUID.randomUUID()).name(name).stage(ContentStage.PRE_PREGNANCY)
                .templateType(ChecklistTemplateType.MANDATORY)
                .recipientScope(com.carebridge.backend.checklist.model.ChecklistRecipientScope.MOTHER)
                .distributionEnabled(true).migrationReviewRequired(false).status(ChecklistTemplateStatus.APPROVED)
                .sequencePosition(position).build();
    }
}
