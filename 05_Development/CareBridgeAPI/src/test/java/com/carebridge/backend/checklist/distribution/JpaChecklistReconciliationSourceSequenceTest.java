package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistRecipientScope;
import com.carebridge.backend.checklist.repository.ChecklistInstanceRepository;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JpaChecklistReconciliationSourceSequenceTest {

    private static final UUID ACTOR = uuid(1);
    private static final UUID JOURNEY = uuid(2);
    private static final UUID CORRELATION = uuid(3);
    private static final UUID POSITION_ONE_VERSION = uuid(11);
    private static final UUID POSITION_TWO_VERSION = uuid(12);
    private static final UUID POSITION_THREE_VERSION = uuid(13);
    private static final UUID LEGACY_VERSION = uuid(20);
    private static final UUID ORPHAN_VERSION = uuid(21);
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ChecklistTemplateRepository templates = mock(ChecklistTemplateRepository.class);
    private final ChecklistItemRepository items = mock(ChecklistItemRepository.class);
    private final CareGroupRepository groups = mock(CareGroupRepository.class);
    private final CareGroupMemberRepository members = mock(CareGroupMemberRepository.class);
    private final MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
    private final BabyProfileRepository babies = mock(BabyProfileRepository.class);
    private final ChecklistInstanceRepository instances = mock(ChecklistInstanceRepository.class);
    private JpaChecklistReconciliationSource source;

    @BeforeEach
    void setUp() {
        source = new JpaChecklistReconciliationSource(
                templates, items, groups, members, journeys, babies, instances);
        when(members.findByUserIdAndInviteStatus(ACTOR, InviteStatus.ACCEPTED)).thenReturn(List.of());
        when(journeys.findCanonical(ACTOR)).thenReturn(Optional.of(MotherJourney.builder()
                .id(JOURNEY)
                .ownerUserId(ACTOR)
                .journeyType(JourneyType.PRE_PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build()));
    }

    @Test
    void noCurrentInstanceEmitsOnlyPositionOne() {
        List<ChecklistTemplate> chain = activeChain();
        stubApproved(chain);
        when(instances.findByRecipientUserIdAndHistoricalAtIsNull(ACTOR)).thenReturn(List.of());

        List<ChecklistDistributionCommand> commands = loadCandidates();

        assertThat(commands).extracting(ChecklistDistributionCommand::templateVersionId)
                .containsExactly(POSITION_ONE_VERSION);
        verify(templates, never()).findAllByTemplateVersionIdIn(any());
        verify(templates, never()).findByTemplateVersionId(any());
    }

    @Test
    void archivedNonhistoricalLegacyPositionZeroDoesNotBlockPositionOne() {
        List<ChecklistTemplate> chain = activeChain();
        ChecklistTemplate archivedLegacy = legacyTemplate(ChecklistTemplateStatus.ARCHIVED, false);
        stubApproved(chain);
        when(instances.findByRecipientUserIdAndHistoricalAtIsNull(ACTOR))
                .thenReturn(List.of(currentInstance(LEGACY_VERSION)));
        when(templates.findAllByTemplateVersionIdIn(List.of(LEGACY_VERSION)))
                .thenReturn(List.of(archivedLegacy));

        List<ChecklistDistributionCommand> commands = loadCandidates();

        assertThat(commands).extracting(ChecklistDistributionCommand::templateVersionId)
                .containsExactly(POSITION_ONE_VERSION);
        verify(templates).findAllByTemplateVersionIdIn(List.of(LEGACY_VERSION));
        verify(templates, never()).findByTemplateVersionId(any());
    }

    @Test
    void unresolvedSystemInstanceDoesNotBlockPositionOne() {
        List<ChecklistTemplate> chain = activeChain();
        stubApproved(chain);
        when(instances.findByRecipientUserIdAndHistoricalAtIsNull(ACTOR))
                .thenReturn(List.of(currentInstance(ORPHAN_VERSION)));
        when(templates.findAllByTemplateVersionIdIn(List.of(ORPHAN_VERSION))).thenReturn(List.of());

        List<ChecklistDistributionCommand> commands = loadCandidates();

        assertThat(commands).extracting(ChecklistDistributionCommand::templateVersionId)
                .containsExactly(POSITION_ONE_VERSION);
        verify(templates).findAllByTemplateVersionIdIn(List.of(ORPHAN_VERSION));
        verify(templates, never()).findByTemplateVersionId(any());
    }

    @Test
    void visibleActiveLegacyPositionZeroKeepsLegacyCohort() {
        ChecklistTemplate activeLegacy = legacyTemplate(ChecklistTemplateStatus.APPROVED, true);
        List<ChecklistTemplate> approved = new java.util.ArrayList<>();
        approved.add(activeLegacy);
        approved.addAll(activeChain());
        stubApproved(approved);
        when(instances.findByRecipientUserIdAndHistoricalAtIsNull(ACTOR))
                .thenReturn(List.of(currentInstance(LEGACY_VERSION)));
        when(templates.findAllByTemplateVersionIdIn(List.of(LEGACY_VERSION)))
                .thenReturn(List.of(activeLegacy));

        List<ChecklistDistributionCommand> commands = loadCandidates();

        assertThat(commands).extracting(ChecklistDistributionCommand::templateVersionId)
                .containsExactly(LEGACY_VERSION);
        verify(templates).findAllByTemplateVersionIdIn(List.of(LEGACY_VERSION));
        verify(templates, never()).findByTemplateVersionId(any());
    }

    private List<ChecklistDistributionCommand> loadCandidates() {
        return source.loadCandidatesForActor(ACTOR, DATE, ZONE, CORRELATION);
    }

    private void stubApproved(List<ChecklistTemplate> approved) {
        when(templates.findAllDistributionEnabledByStatus(ChecklistTemplateStatus.APPROVED))
                .thenReturn(approved);
        approved.forEach(template ->
                when(items.findByTemplate_IdOrderByOrder(template.getId())).thenReturn(List.of()));
    }

    private static List<ChecklistTemplate> activeChain() {
        return List.of(
                sequenceTemplate(POSITION_ONE_VERSION, 1),
                sequenceTemplate(POSITION_TWO_VERSION, 2),
                sequenceTemplate(POSITION_THREE_VERSION, 3));
    }

    private static ChecklistTemplate sequenceTemplate(UUID versionId, int position) {
        return baseTemplate(versionId)
                .sequencePosition(position)
                .status(ChecklistTemplateStatus.APPROVED)
                .distributionEnabled(true)
                .build();
    }

    private static ChecklistTemplate legacyTemplate(
            ChecklistTemplateStatus status, boolean distributionEnabled) {
        return baseTemplate(LEGACY_VERSION)
                .sequencePosition(0)
                .status(status)
                .distributionEnabled(distributionEnabled)
                .build();
    }

    private static ChecklistTemplate.ChecklistTemplateBuilder baseTemplate(UUID versionId) {
        return ChecklistTemplate.builder()
                .id(UUID.randomUUID())
                .templateLineageId(UUID.randomUUID())
                .templateVersionId(versionId)
                .name("Template " + versionId)
                .stage(ContentStage.PRE_PREGNANCY)
                .recipientScope(ChecklistRecipientScope.MOTHER)
                .templateType(ChecklistTemplateType.MANDATORY)
                .migrationReviewRequired(false)
                .eligibilityAnchorType(ChecklistAnchorType.NONE)
                .eligibilityRangeUnit(ChecklistRangeUnit.DAY)
                .eligibilityStartInclusive(0)
                .eligibilityEndInclusive(0);
    }

    private static ChecklistInstance currentInstance(UUID versionId) {
        return ChecklistInstance.builder()
                .id(UUID.randomUUID())
                .templateVersionId(versionId)
                .recipientUserId(ACTOR)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(JOURNEY)
                .contextOwnerUserId(ACTOR)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .status(ChecklistInstanceStatus.PENDING)
                .build();
    }

    private static UUID uuid(int suffix) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", suffix));
    }
}
