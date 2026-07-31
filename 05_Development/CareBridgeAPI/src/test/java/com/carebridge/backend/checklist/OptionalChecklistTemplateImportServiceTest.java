package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionCommand;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionResult;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionService;
import com.carebridge.backend.checklist.dto.SelfAssignChecklistTemplateRequest;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistRecipientScope;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.service.OptionalChecklistTemplateImportService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptionalChecklistTemplateImportServiceTest {

    @Mock ChecklistTemplateRepository templateRepository;
    @Mock ChecklistItemRepository itemRepository;
    @Mock MotherJourneyRepository journeyRepository;
    @Mock BabyProfileRepository babyRepository;
    @Mock ChecklistDistributionService distributionService;
    @InjectMocks OptionalChecklistTemplateImportService service;

    @Test
    void approvedOptionalPrePregnancyTemplateCreatesIdempotentV2DistributionCommand() {
        UUID actorId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID substageId = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        ChecklistTemplate template = ChecklistTemplate.builder()
                .id(templateId).templateLineageId(templateId).templateVersionId(versionId)
                .name("Prepare for pregnancy").stage(ContentStage.PRE_PREGNANCY)
                .substageId(substageId).status(ChecklistTemplateStatus.APPROVED)
                .templateType(ChecklistTemplateType.OPTIONAL).migrationReviewRequired(false)
                .distributionEnabled(false).recipientScope(ChecklistRecipientScope.MOTHER)
                .eligibilityAnchorType(ChecklistAnchorType.NONE)
                .eligibilityRangeUnit(ChecklistRangeUnit.DAY)
                .eligibilityStartInclusive(0).eligibilityEndInclusive(0).build();
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(journeyRepository.findCanonical(actorId)).thenReturn(Optional.of(MotherJourney.builder()
                .id(journeyId).ownerUserId(actorId).journeyType(JourneyType.PRE_PREGNANCY)
                .status(JourneyStatus.ACTIVE).build()));
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByTemplate_IdOrderByOrder(templateId)).thenReturn(List.of(
                ChecklistItem.builder().id(itemId).template(template).itemText("Book a preconception visit")
                        .order(1).isRequired(false).isActive(true)
                        .targetSubject(ChecklistTargetSubject.MOTHER).build()));
        when(distributionService.distribute(any())).thenReturn(ChecklistDistributionResult.created(1, 1));

        ChecklistDistributionResult result = service.selfAssign(
                new SelfAssignChecklistTemplateRequest(templateId, null, null), actorId);

        assertThat(result.createdInstances()).isOne();
        assertThat(result.createdTasks()).isOne();
        ArgumentCaptor<ChecklistDistributionCommand> command =
                ArgumentCaptor.forClass(ChecklistDistributionCommand.class);
        verify(distributionService).distribute(command.capture());
        assertThat(command.getValue().templateVersionId()).isEqualTo(versionId);
        assertThat(command.getValue().careGroupId()).isNull();
        assertThat(command.getValue().careGroupOwnerUserId()).isEqualTo(actorId);
        assertThat(command.getValue().contextId()).isEqualTo(journeyId);
        assertThat(command.getValue().stage()).isEqualTo(ContentStage.PRE_PREGNANCY);
        assertThat(command.getValue().substage()).isNotNull();
        assertThat(command.getValue().substage().getAnchorType()).isEqualTo(ChecklistAnchorType.NONE);
        assertThat(command.getValue().substage().getEndInclusive()).isZero();
        assertThat(command.getValue().recipients()).singleElement()
                .extracting(recipient -> recipient.role()).isEqualTo(ChecklistRecipientRole.MOTHER);
        assertThat(command.getValue().items()).singleElement()
                .extracting(item -> item.templateItemVersionId()).isEqualTo(itemId);
    }

    @Test
    void mandatoryTemplateCannotBeSelfAssigned() {
        UUID actorId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(ChecklistTemplate.builder()
                .id(templateId).status(ChecklistTemplateStatus.APPROVED)
                .templateType(ChecklistTemplateType.MANDATORY).build()));

        assertThatThrownBy(() -> service.selfAssign(
                new SelfAssignChecklistTemplateRequest(templateId, null, null), actorId))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("CHECKLIST_TEMPLATE_UNAVAILABLE"));
        verifyNoInteractions(distributionService);
    }
}
