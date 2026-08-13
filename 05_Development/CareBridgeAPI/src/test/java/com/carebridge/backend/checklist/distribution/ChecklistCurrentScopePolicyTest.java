package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.entity.GestationalDatingBasis;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChecklistCurrentScopePolicyTest {

    private static final UUID OWNER = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID JOURNEY = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID VERSION = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final LocalDate LMP = LocalDate.of(2026, 1, 1);
    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 1, 8);

    private final ChecklistTemplateRepository templates = mock(ChecklistTemplateRepository.class);
    private final MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
    private final BabyProfileRepository babies = mock(BabyProfileRepository.class);
    private ChecklistCurrentScopePolicy policy;
    private ChecklistTemplate template;
    private MotherJourney journey;

    @BeforeEach
    void setUp() {
        policy = new ChecklistCurrentScopePolicy(templates, journeys, babies);
        template = ChecklistTemplate.builder()
                .templateVersionId(VERSION)
                .stage(ContentStage.PREGNANCY)
                .eligibilityAnchorType(com.carebridge.backend.checklist.model.ChecklistAnchorType.LMP)
                .eligibilityRangeUnit(com.carebridge.backend.checklist.model.ChecklistRangeUnit.WEEK)
                .eligibilityStartInclusive(0)
                .eligibilityEndInclusive(40)
                .build();
        journey = MotherJourney.builder()
                .id(JOURNEY)
                .ownerUserId(OWNER)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .lastMenstrualDate(LMP)
                .estimatedDueDate(LMP.plusDays(280))
                .gestationalDatingBasis(GestationalDatingBasis.LMP)
                .gestationalDatingRevision(2L)
                .gestationalDatingEffectiveAt(Instant.parse("2026-01-02T00:00:00Z"))
                .build();
        when(templates.findByTemplateVersionId(VERSION)).thenReturn(Optional.of(template));
        when(journeys.findCanonical(OWNER)).thenReturn(Optional.of(journey));
    }

    @Test
    void priorDatingRevisionIsHistoricalEvenWhenWindowMatches() {
        ChecklistInstance instance = instance(1L);

        assertThat(policy.isCurrent(instance, EFFECTIVE_DATE)).isFalse();
    }

    @Test
    void matchingDatingRevisionRemainsCurrent() {
        ChecklistInstance instance = instance(2L);

        assertThat(policy.isCurrent(instance, EFFECTIVE_DATE)).isTrue();
    }

    @Test
    void configuredButUnsupportedCadenceFailsClosed() {
        template.setScheduleType(ChecklistScheduleType.WEEKLY);
        template.setMaterializationPolicy(ChecklistMaterializationPolicy.EACH_DAY);
        ChecklistInstance instance = instance(2L);
        instance.setPeriodKey("legacy-period");

        assertThat(policy.isCurrent(instance, EFFECTIVE_DATE)).isFalse();
    }

    private ChecklistInstance instance(Long revision) {
        return ChecklistInstance.builder()
                .templateVersionId(VERSION)
                .recipientUserId(OWNER)
                .recipientRole(ChecklistRecipientRole.MOTHER)
                .careContextType(ChecklistCareContextType.JOURNEY)
                .careContextId(JOURNEY)
                .contextOwnerUserId(OWNER)
                .origin(ChecklistOrigin.SYSTEM_TEMPLATE)
                .status(ChecklistInstanceStatus.PENDING)
                .gestationalDatingRevision(revision)
                .windowStart(LMP)
                .windowEnd(LMP.plusWeeks(40))
                .build();
    }
}
