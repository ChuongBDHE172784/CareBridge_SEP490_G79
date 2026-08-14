package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import com.carebridge.backend.checklist.policy.ChecklistTemplateVisibilityPolicy;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.GestationalDatingResolver;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChecklistCurrentScopePolicy {

    private final ChecklistTemplateRepository templateRepository;
    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyRepository;
    private final ChecklistLifecycleEligibilityService eligibilityService = new ChecklistLifecycleEligibilityService();

    public boolean isHistoryManaged(ChecklistInstance instance) {
        return instance != null
                && instance.getRecipientRole() == ChecklistRecipientRole.MOTHER
                && instance.getOrigin() == ChecklistOrigin.SYSTEM_TEMPLATE
                && instance.getCareGroupId() == null
                && Objects.equals(instance.getRecipientUserId(), instance.getContextOwnerUserId())
                && instance.getTemplateVersionId() != null
                && instance.getCareContextType() != null
                && instance.getCareContextId() != null
                && instance.getContextOwnerUserId() != null;
    }

    @Transactional(readOnly = true)
    public boolean isArchivedTemplate(ChecklistInstance instance) {
        if (instance == null || instance.getTemplateVersionId() == null) {
            return false;
        }
        ChecklistTemplate template = templateRepository.findByTemplateVersionId(
                instance.getTemplateVersionId()).orElse(null);
        return ChecklistTemplateVisibilityPolicy.isArchived(template);
    }

    @Transactional(readOnly = true)
    public boolean isCurrent(ChecklistInstance instance, LocalDate effectiveDate) {
        if (!isHistoryManaged(instance) || effectiveDate == null) {
            return true;
        }
        if (instance.getTemplateVersionId() == null
                || instance.getCareContextType() == null
                || instance.getCareContextId() == null
                || instance.getContextOwnerUserId() == null) {
            return false;
        }
        ChecklistTemplate template = templateRepository.findByTemplateVersionId(instance.getTemplateVersionId())
                .orElse(null);
        if (template == null) {
            return false;
        }
        return switch (instance.getCareContextType()) {
            case JOURNEY -> isCurrentJourneyInstance(instance, template, effectiveDate);
            case BABY -> isCurrentBabyInstance(instance, template, effectiveDate);
        };
    }

    private boolean isCurrentJourneyInstance(
            ChecklistInstance instance,
            ChecklistTemplate template,
            LocalDate effectiveDate) {
        MotherJourney journey = journeyRepository.findCanonical(instance.getContextOwnerUserId()).orElse(null);
        if (journey == null || !instance.getCareContextId().equals(journey.getId())) {
            return false;
        }
        if (journey.getJourneyType() == null) {
            return false;
        }
        ContentStage stage = template.getStage();
        if (stage != null && !stage.name().equals(journey.getJourneyType().name())) {
            return false;
        }
        if (journey.getJourneyType() == com.carebridge.backend.journey.entity.JourneyType.PREGNANCY
                && !GestationalDatingResolver.hasResolvedAuthority(journey)) {
            // Raw legacy LMP/EDD pairs are not cadence authority. They remain
            // readable through compatibility surfaces but cannot make a
            // current pregnancy occurrence eligible.
            return false;
        }
        if (journey.getJourneyType() == com.carebridge.backend.journey.entity.JourneyType.PREGNANCY
                && !Objects.equals(
                        instance.getGestationalDatingRevision(),
                        journey.getGestationalDatingRevision())) {
            // A dating correction opens a new occurrence identity. Even when
            // the recalculated calendar window happens to be unchanged, an
            // instance from the prior authority revision is historical.
            return false;
        }
        ChecklistLifecycleDates dates = new ChecklistLifecycleDates(
                journey.getLastMenstrualDate(),
                journey.getEstimatedDueDate(),
                journey.getDeliveryDate(),
                null);
        return hasSameEligibleWindow(instance, template, dates, effectiveDate);
    }

    private boolean isCurrentBabyInstance(
            ChecklistInstance instance,
            ChecklistTemplate template,
            LocalDate effectiveDate) {
        BabyProfile baby = babyRepository.findByIdAndOwnerUserId(
                        instance.getCareContextId(), instance.getContextOwnerUserId())
                .filter(candidate -> candidate.getStatus() == BabyProfileStatus.ACTIVE)
                .orElse(null);
        if (baby == null) {
            return false;
        }
        ContentStage stage = template.getStage();
        if (stage != ContentStage.BABY_CARE) {
            return false;
        }
        ChecklistLifecycleDates dates = new ChecklistLifecycleDates(null, null, null, baby.getBirthDate());
        return hasSameEligibleWindow(instance, template, dates, effectiveDate);
    }

    private boolean hasSameEligibleWindow(
            ChecklistInstance instance,
            ChecklistTemplate template,
            ChecklistLifecycleDates dates,
            LocalDate effectiveDate) {
        try {
            ChecklistLifecycleEligibility currentEligibility = eligibilityForCurrentOccurrence(
                    instance, template, dates, effectiveDate);
            ChecklistEligibilityDecision decision = eligibilityService.evaluate(
                    template.getStage(), currentEligibility, dates, effectiveDate);
            boolean sameWindow = decision.eligible()
                    && Objects.equals(instance.getWindowStart(), decision.windowStart())
                    && Objects.equals(instance.getWindowEnd(), decision.windowEnd());
            if (!sameWindow || instance.getPeriodKey() == null) {
                return sameWindow;
            }
            String expectedPeriodKey = ChecklistPeriodIdentity.periodKey(
                    template.getScheduleType(), template.getMaterializationPolicy(),
                    decision, effectiveDate);
            boolean cadenceConfigured = template.getScheduleType() != null
                    || template.getMaterializationPolicy() != null;
            if (cadenceConfigured && expectedPeriodKey == null) {
                return false;
            }
            return expectedPeriodKey == null || instance.getPeriodKey().equals(expectedPeriodKey);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static ChecklistLifecycleEligibility eligibilityForCurrentOccurrence(
            ChecklistInstance instance,
            ChecklistTemplate template,
            ChecklistLifecycleDates dates,
            LocalDate effectiveDate) {
        if (template.getScheduleType() != ChecklistScheduleType.WEEKLY
                || template.getMaterializationPolicy() != ChecklistMaterializationPolicy.EACH_WEEK
                || template.getStage() != ContentStage.PREGNANCY) {
            return eligibility(template);
        }
        LocalDate canonicalLmp = dates.lastMenstrualDate() != null
                ? dates.lastMenstrualDate()
                : dates.estimatedDueDate() == null
                        ? null : dates.estimatedDueDate().minusDays(280);
        if (canonicalLmp == null || effectiveDate.isBefore(canonicalLmp)) {
            return eligibility(template);
        }
        long completedWeek = ChronoUnit.DAYS.between(canonicalLmp, effectiveDate) / 7;
        Integer start = template.getEligibilityStartInclusive();
        Integer end = template.getEligibilityEndInclusive();
        if (start == null || end == null || completedWeek < start
                || (end != Integer.MAX_VALUE && completedWeek > end)) {
            return eligibility(template);
        }
        int week = Math.toIntExact(completedWeek);
        return new TemplateLifecycleEligibility(ContentStage.PREGNANCY.name(),
                ChecklistAnchorType.LMP, ChecklistRangeUnit.WEEK, week, week, true);
    }

    private static ChecklistLifecycleEligibility eligibility(ChecklistTemplate template) {
        if (template.getStage() == null) {
            return null;
        }
        return new TemplateLifecycleEligibility(
                template.getStage().name(),
                template.getEligibilityAnchorType(),
                template.getEligibilityRangeUnit(),
                template.getEligibilityStartInclusive(),
                template.getEligibilityEndInclusive(),
                true);
    }

    private record TemplateLifecycleEligibility(
            String stage,
            ChecklistAnchorType anchorType,
            ChecklistRangeUnit rangeUnit,
            Integer startInclusive,
            Integer endInclusive,
            Boolean active) implements ChecklistLifecycleEligibility {

        @Override
        public String getStage() {
            return stage;
        }

        @Override
        public ChecklistAnchorType getAnchorType() {
            return anchorType;
        }

        @Override
        public ChecklistRangeUnit getRangeUnit() {
            return rangeUnit;
        }

        @Override
        public Integer getStartInclusive() {
            return startInclusive;
        }

        @Override
        public Integer getEndInclusive() {
            return endInclusive;
        }

        @Override
        public Boolean getActive() {
            return active;
        }
    }
}
