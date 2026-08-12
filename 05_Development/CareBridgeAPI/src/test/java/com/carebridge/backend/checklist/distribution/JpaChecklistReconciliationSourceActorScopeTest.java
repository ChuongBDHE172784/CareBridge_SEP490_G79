package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistRecipientScope;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import com.carebridge.backend.checklist.model.ChecklistWeekBoundaryRule;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.GestationalDatingBasis;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JpaChecklistReconciliationSourceActorScopeTest {

    private static final UUID ACTOR = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID OWNER = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID PERSONAL_JOURNEY = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID FAMILY_JOURNEY = UUID.fromString("20000000-0000-0000-0000-000000000004");
    private static final UUID GROUP = UUID.fromString("20000000-0000-0000-0000-000000000005");
    private static final UUID VERSION = UUID.fromString("20000000-0000-0000-0000-000000000006");
    private static final UUID CORRELATION = UUID.fromString("20000000-0000-0000-0000-000000000008");
    private static final LocalDate DATE = LocalDate.of(2026, 7, 31);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ChecklistTemplateRepository templates = mock(ChecklistTemplateRepository.class);
    private final ChecklistItemRepository items = mock(ChecklistItemRepository.class);
    private final CareGroupRepository groups = mock(CareGroupRepository.class);
    private final CareGroupMemberRepository members = mock(CareGroupMemberRepository.class);
    private final MotherJourneyRepository journeys = mock(MotherJourneyRepository.class);
    private final BabyProfileRepository babies = mock(BabyProfileRepository.class);
    private JpaChecklistReconciliationSource source;
    private ChecklistTemplate template;

    @BeforeEach
    void setUp() {
        source = new JpaChecklistReconciliationSource(templates, items, groups, members, journeys, babies);
        template = ChecklistTemplate.builder()
                .id(UUID.randomUUID()).templateLineageId(UUID.randomUUID()).templateVersionId(VERSION)
                .status(ChecklistTemplateStatus.APPROVED).distributionEnabled(true)
                .migrationReviewRequired(false).stage(ContentStage.PREGNANCY)
                .recipientScope(ChecklistRecipientScope.BOTH).eligibilityAnchorType(ChecklistAnchorType.LMP)
                .eligibilityRangeUnit(ChecklistRangeUnit.WEEK).eligibilityStartInclusive(0)
                .eligibilityEndInclusive(40).build();
        when(templates.findAllDistributionEnabledByStatus(ChecklistTemplateStatus.APPROVED))
                .thenReturn(List.of(template));
        when(items.findByTemplate_IdOrderByOrder(template.getId())).thenReturn(List.of());
        when(members.findByUserIdAndInviteStatus(ACTOR, InviteStatus.ACCEPTED)).thenReturn(List.of(
                CareGroupMember.builder().id(UUID.randomUUID()).careGroupId(GROUP).userId(ACTOR)
                        .checklistAccessEpoch(0L).inviteStatus(InviteStatus.ACCEPTED)
                        .permissionJson("{\"CHECKLIST_VIEW\":true,\"CHECKLIST_COMPLETE\":false}").build()));
        when(groups.findByIdAndStatus(GROUP, CareGroupStatus.ACTIVE)).thenReturn(Optional.of(
                CareGroup.builder().id(GROUP).ownerUserId(OWNER).status(CareGroupStatus.ACTIVE)
                        .linkedJourneyId(FAMILY_JOURNEY).build()));
        when(journeys.findCanonical(ACTOR)).thenReturn(Optional.of(journey(PERSONAL_JOURNEY, ACTOR)));
        when(journeys.findById(FAMILY_JOURNEY)).thenReturn(Optional.of(journey(FAMILY_JOURNEY, OWNER)));
    }

    @Test
    void candidatesUseDirectCanonicalLinksWithInlineEligibility() {
        List<ChecklistDistributionCommand> result =
                source.loadCandidatesForActor(ACTOR, DATE, ZONE, CORRELATION);

        assertActorOnlyPersonalAndFamily(result);
        verify(journeys, never()).findByStatus(any());
        verify(groups, never()).findByStatus(any());
    }

    @Test
    void ownerMembershipNeverCreatesAnExtraFamilyAssignment() {
        when(members.findByUserIdAndInviteStatus(ACTOR, InviteStatus.ACCEPTED)).thenReturn(List.of(
                CareGroupMember.builder().id(UUID.randomUUID()).careGroupId(GROUP).userId(ACTOR)
                        .checklistAccessEpoch(0L)
                        .memberRole(GroupMemberRole.OWNER).inviteStatus(InviteStatus.ACCEPTED)
                        .permissionJson("{\"CHECKLIST_VIEW\":true,\"CHECKLIST_COMPLETE\":true}").build()));

        List<ChecklistDistributionCommand> result =
                source.loadCandidatesForActor(ACTOR, DATE, ZONE, CORRELATION);

        assertThat(result).singleElement().satisfies(command -> {
            assertThat(command.careGroupId()).isNull();
            assertThat(command.recipients().get(0).role()).isEqualTo(ChecklistRecipientRole.MOTHER);
        });
        verify(groups, never()).findByIdAndStatus(any(), any());
    }

    @Test
    void unresolvedPregnancyAuthorityIsNotMaterializedForAnyRecipient() {
        MotherJourney unresolved = journey(PERSONAL_JOURNEY, ACTOR).toBuilder()
                .gestationalDatingRevision(null)
                .build();
        MotherJourney unresolvedFamily = journey(FAMILY_JOURNEY, OWNER).toBuilder()
                .gestationalDatingQuarantineReasonCode("DATING_DISCREPANCY")
                .build();
        when(journeys.findCanonical(ACTOR)).thenReturn(Optional.of(unresolved));
        when(journeys.findById(FAMILY_JOURNEY)).thenReturn(Optional.of(unresolvedFamily));

        assertThat(source.loadCandidatesForActor(ACTOR, DATE, ZONE, CORRELATION)).isEmpty();
    }

    @Test
    void weeklyTemplateUsesTheCurrentAnchorRelativeWeekAndV2PeriodIdentity() {
        template.setScheduleType(ChecklistScheduleType.WEEKLY);
        template.setMaterializationPolicy(ChecklistMaterializationPolicy.EACH_WEEK);
        template.setScheduleGroupKey("PREGNANCY_WHO_PLAN_01");
        template.setScheduleContextType(ChecklistCareContextType.JOURNEY);
        template.setWeekBoundaryRule(ChecklistWeekBoundaryRule.ANCHOR_RELATIVE_7D);
        template.setEligibilityEndInclusive(40);

        List<ChecklistDistributionCommand> result =
                source.loadCandidatesForActor(ACTOR, DATE, ZONE, CORRELATION);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(command -> {
            assertThat(command.substage().getStartInclusive()).isEqualTo(30);
            assertThat(command.substage().getEndInclusive()).isEqualTo(30);
            assertThat(command.cadence()).isNotNull();
            assertThat(command.cadence().scheduleType()).isEqualTo(ChecklistScheduleType.WEEKLY);
            assertThat(command.cadence().materializationPolicy())
                    .isEqualTo(ChecklistMaterializationPolicy.EACH_WEEK);
            assertThat(command.cadence().periodKey()).isEqualTo("W:G:0030:2026-07-30");
            assertThat(command.cadence().scheduleZone()).isEqualTo(ZONE);
        });
    }

    private static void assertActorOnlyPersonalAndFamily(List<ChecklistDistributionCommand> result) {
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ChecklistDistributionCommand::contextId)
                .containsExactlyInAnyOrder(PERSONAL_JOURNEY, FAMILY_JOURNEY);
        assertThat(result).allSatisfy(command ->
                assertThat(command.recipients()).extracting(ChecklistDistributionRecipient::userId)
                        .containsExactly(ACTOR));
        assertThat(result).filteredOn(command -> command.careGroupId() == null)
                .singleElement().satisfies(command ->
                        assertThat(command.recipients().get(0).role()).isEqualTo(ChecklistRecipientRole.MOTHER));
        assertThat(result).filteredOn(command -> GROUP.equals(command.careGroupId()))
                .singleElement().satisfies(command -> {
                    ChecklistDistributionRecipient recipient = command.recipients().get(0);
                    assertThat(recipient.role()).isEqualTo(ChecklistRecipientRole.FAMILY);
                    assertThat(recipient.checklistView()).isTrue();
                    assertThat(recipient.checklistComplete()).isFalse();
                });
        assertThat(result).allSatisfy(command -> {
            assertThat(command.stage()).isEqualTo(ContentStage.PREGNANCY);
            assertThat(command.substage()).isNotNull();
            assertThat(command.substage().getStage()).isEqualTo("PREGNANCY");
            assertThat(command.substage().getAnchorType()).isEqualTo(ChecklistAnchorType.LMP);
            assertThat(command.substage().getRangeUnit()).isEqualTo(ChecklistRangeUnit.WEEK);
            assertThat(command.substage().getStartInclusive()).isZero();
            assertThat(command.substage().getEndInclusive()).isEqualTo(40);
        });
    }

    private static MotherJourney journey(UUID id, UUID owner) {
        return MotherJourney.builder().id(id).ownerUserId(owner).status(JourneyStatus.ACTIVE)
                .journeyType(JourneyType.PREGNANCY).lastMenstrualDate(LocalDate.of(2026, 1, 1))
                .estimatedDueDate(LocalDate.of(2026, 10, 8))
                .gestationalDatingBasis(GestationalDatingBasis.LMP)
                .gestationalDatingRevision(1L)
                .gestationalDatingEffectiveAt(DATE.atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();
    }

}
