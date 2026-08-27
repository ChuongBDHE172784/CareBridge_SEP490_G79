package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistInstanceStatus;
import com.carebridge.backend.checklist.model.ChecklistMaterializationMode;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    @Test
    void exactPrePregnancySequenceIdentityRemainsCurrent() {
        configurePrePregnancySequence();

        assertThat(policy.isCurrent(prePregnancySequenceInstance(), EFFECTIVE_DATE)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedSequenceIdentities")
    void malformedPrePregnancySequenceIdentityFailsClosed(
            String scenario,
            Short contractVersion,
            String periodKey,
            String zoneId,
            ChecklistMaterializationMode mode,
            Boolean wasActionable) {
        configurePrePregnancySequence();
        ChecklistInstance instance = prePregnancySequenceInstance();
        instance.setChecklistContractVersion(contractVersion);
        instance.setPeriodKey(periodKey);
        instance.setScheduleZoneId(zoneId);
        instance.setMaterializationMode(mode);
        instance.setWasActionable(wasActionable);

        assertThat(policy.isCurrent(instance, EFFECTIVE_DATE)).isFalse();
    }

    @Test
    void prePregnancyUnsupportedSchedulePolicyPairDoesNotUseNonCadenceBypass() {
        configurePrePregnancySequence();
        template.setMaterializationPolicy(ChecklistMaterializationPolicy.EACH_DAY);

        assertThat(policy.isCurrent(prePregnancySequenceInstance(), EFFECTIVE_DATE)).isFalse();
    }

    @Test
    void prePregnancyUnsupportedPairWithMissingPeriodKeyFailsClosed() {
        configurePrePregnancySequence();
        template.setMaterializationPolicy(ChecklistMaterializationPolicy.EACH_DAY);
        ChecklistInstance instance = prePregnancySequenceInstance();
        instance.setPeriodKey(null);

        assertThat(policy.isCurrent(instance, EFFECTIVE_DATE)).isFalse();
    }

    @Test
    void legacyV1PrePregnancySequenceKeepsItsNullableIdentity() {
        configurePrePregnancySequence();
        template.setChecklistContractVersion(ChecklistPeriodIdentity.V1_CONTRACT_VERSION);
        ChecklistInstance instance = prePregnancySequenceInstance();
        instance.setChecklistContractVersion(null);
        instance.setPeriodKey(null);
        instance.setScheduleZoneId(null);
        instance.setMaterializationMode(null);
        instance.setWasActionable(null);

        assertThat(policy.isCurrent(instance, EFFECTIVE_DATE)).isTrue();
    }

    @Test
    void legacyV1TemplateRejectsAV2NonCadenceTuple() {
        configurePrePregnancySequence();
        template.setChecklistContractVersion(ChecklistPeriodIdentity.V1_CONTRACT_VERSION);

        assertThat(policy.isCurrent(prePregnancySequenceInstance(), EFFECTIVE_DATE)).isFalse();
    }

    @Test
    void validWeeklyIdentityStillMatchesTheCurrentOccurrence() {
        template.setScheduleType(ChecklistScheduleType.WEEKLY);
        template.setMaterializationPolicy(ChecklistMaterializationPolicy.EACH_WEEK);
        ChecklistInstance instance = instance(2L);
        instance.setWindowStart(EFFECTIVE_DATE);
        instance.setWindowEnd(EFFECTIVE_DATE);
        instance.setPeriodKey("W:G:0001:2026-01-08");

        assertThat(policy.isCurrent(instance, EFFECTIVE_DATE)).isTrue();
    }

    @Test
    void configuredWeeklyIdentityWithoutPeriodKeyFailsClosed() {
        template.setScheduleType(ChecklistScheduleType.WEEKLY);
        template.setMaterializationPolicy(ChecklistMaterializationPolicy.EACH_WEEK);
        ChecklistInstance instance = instance(2L);
        instance.setWindowStart(EFFECTIVE_DATE);
        instance.setWindowEnd(EFFECTIVE_DATE);

        assertThat(policy.isCurrent(instance, EFFECTIVE_DATE)).isFalse();
    }

    @Test
    void legacyWindowWithoutPeriodKeyKeepsCompatibilityBehavior() {
        template.setScheduleType(ChecklistScheduleType.LEGACY);
        template.setMaterializationPolicy(ChecklistMaterializationPolicy.LEGACY_WINDOW);

        assertThat(policy.isCurrent(instance(2L), EFFECTIVE_DATE)).isTrue();
    }

    private void configurePrePregnancySequence() {
        template.setStage(ContentStage.PRE_PREGNANCY);
        template.setEligibilityAnchorType(ChecklistAnchorType.NONE);
        template.setEligibilityRangeUnit(ChecklistRangeUnit.DAY);
        template.setEligibilityStartInclusive(0);
        template.setEligibilityEndInclusive(0);
        template.setScheduleType(ChecklistScheduleType.SET);
        template.setMaterializationPolicy(ChecklistMaterializationPolicy.SEQUENCE_STEP);
        template.setChecklistContractVersion(ChecklistPeriodIdentity.V2_CONTRACT_VERSION);
        journey.setJourneyType(JourneyType.PRE_PREGNANCY);
    }

    private ChecklistInstance prePregnancySequenceInstance() {
        ChecklistInstance instance = instance(null);
        instance.setWindowStart(null);
        instance.setWindowEnd(null);
        instance.setChecklistContractVersion(ChecklistPeriodIdentity.V2_CONTRACT_VERSION);
        instance.setPeriodKey(ChecklistPeriodIdentity.V2_NON_CADENCE_PERIOD_KEY);
        instance.setScheduleZoneId(ChecklistPeriodIdentity.V2_NON_CADENCE_ZONE_ID);
        instance.setMaterializationMode(ChecklistPeriodIdentity.V2_NON_CADENCE_MODE);
        instance.setWasActionable(true);
        return instance;
    }

    private static Stream<Arguments> malformedSequenceIdentities() {
        short v2 = ChecklistPeriodIdentity.V2_CONTRACT_VERSION;
        String period = ChecklistPeriodIdentity.V2_NON_CADENCE_PERIOD_KEY;
        String zone = ChecklistPeriodIdentity.V2_NON_CADENCE_ZONE_ID;
        ChecklistMaterializationMode mode = ChecklistPeriodIdentity.V2_NON_CADENCE_MODE;
        return Stream.of(
                Arguments.of("contract differs", (short) 1, period, zone, mode, true),
                Arguments.of("period differs", v2, "O:OTHER", zone, mode, true),
                Arguments.of("zone differs", v2, period, "Asia/Ho_Chi_Minh", mode, true),
                Arguments.of("mode differs", v2, period, zone,
                        ChecklistMaterializationMode.EVENT, true),
                Arguments.of("actionable differs", v2, period, zone, mode, false));
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
