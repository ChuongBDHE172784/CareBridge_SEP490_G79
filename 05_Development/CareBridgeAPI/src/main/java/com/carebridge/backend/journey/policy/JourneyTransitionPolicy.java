package com.carebridge.backend.journey.policy;

import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JourneyTransitionPolicy {

    public static final List<JourneyType> CANONICAL_STAGES = List.of(
            JourneyType.PRE_PREGNANCY,
            JourneyType.PREGNANCY,
            JourneyType.POSTPARTUM);

    public void validateCreate(CreateJourneyRequest request) {
        if (!isCanonical(request.getJourneyType())) {
            throw invalidTransition();
        }
        if (hasCreateDate(request)
                && !isV2(request.getChecklistContractVersion())
                && (request.getDateSource() == null || request.getDateConfidence() == null)) {
            throw missingProvenance();
        }
    }

    public void validateUpdate(JourneyType currentStage, UpdateJourneyRequest request) {
        JourneyType requestedStage = request.getJourneyType();
        if (requestedStage != null && requestedStage != currentStage) {
            boolean allowed = currentStage == JourneyType.PRE_PREGNANCY
                    && requestedStage == JourneyType.PREGNANCY
                    || currentStage == JourneyType.POSTPARTUM
                    && requestedStage == JourneyType.PREGNANCY;
            if (!allowed) {
                throw invalidTransition();
            }
        }
        if (hasUpdateDate(request)
                && !isV2(request.getChecklistContractVersion())
                && (request.getDateSource() == null || request.getDateConfidence() == null)) {
            throw missingProvenance();
        }
    }

    public boolean isCanonical(JourneyType type) {
        return type != null && CANONICAL_STAGES.contains(type);
    }

    public JourneyType outcomeTargetStage(
            JourneyType currentStage,
            PregnancyOutcomeType outcome,
            boolean correction) {
        if (outcome == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "PREGNANCY_OUTCOME_INVALID",
                    "Pregnancy outcome is required");
        }
        if (currentStage == JourneyType.PREGNANCY) {
            return outcome.transitionsToPostpartum()
                    ? JourneyType.POSTPARTUM
                    : JourneyType.PREGNANCY;
        }
        if (currentStage == JourneyType.POSTPARTUM
                && correction
                && outcome.transitionsToPostpartum()) {
            return JourneyType.POSTPARTUM;
        }
        throw new BusinessException(
                HttpStatus.CONFLICT,
                "OUTCOME_STAGE_CONFLICT",
                "Outcome is incompatible with the current lifecycle stage");
    }

    private boolean hasCreateDate(CreateJourneyRequest request) {
        return request.getLastMenstrualDate() != null
                || request.getEstimatedDueDate() != null
                || request.getDatingBasis() != null;
    }

    private boolean hasUpdateDate(UpdateJourneyRequest request) {
        return request.getLastMenstrualDate() != null
                || request.getEstimatedDueDate() != null
                || request.getDeliveryDate() != null
                || request.getDatingBasis() != null;
    }

    private BusinessException invalidTransition() {
        return new BusinessException(
                HttpStatus.CONFLICT, "JOURNEY-016", "Invalid lifecycle transition");
    }

    private BusinessException missingProvenance() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "JOURNEY-018",
                "Date source and confidence are required");
    }

    private boolean isV2(Integer contractVersion) {
        return contractVersion != null && contractVersion == 2;
    }
}
