package com.carebridge.backend.baby.policy;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.entity.*;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.PregnancyOutcomeEvidenceRepository;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BabyJourneyLinkagePolicy {
    private final UserRepository userRepository;
    private final MotherJourneyRepository journeyRepository;
    private final PregnancyOutcomeEvidenceRepository evidenceRepository;

    public MotherJourney requireEligibleJourney(UUID journeyId, UUID ownerId) {
        var user = userRepository.findById(ownerId)
                .orElseThrow(() -> neutralNotFound());
        if (user.getRole() != Role.MOTHER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "LINK_ROLE_REQUIRED", "Mother role required");
        }
        MotherJourney canonical = journeyRepository.findCanonicalForUpdate(ownerId)
                .orElseThrow(this::neutralNotFound);
        if (!canonical.getId().equals(journeyId)) throw neutralNotFound();
        MotherJourney selected = canonical;
        if (!selected.getId().equals(canonical.getId())
                || selected.getStatus() != JourneyStatus.ACTIVE
                || selected.getJourneyType() != JourneyType.POSTPARTUM
                || selected.getPregnancyOutcome() != PregnancyOutcomeType.LIVE_BIRTH) {
            throw notEligible();
        }
        var evidence = evidenceRepository.findFirstByJourneyIdOrderByRevisionNumberDesc(selected.getId())
                .orElseThrow(BabyJourneyLinkagePolicy::notEligible);
        if (!ownerId.equals(evidence.getOwnerUserId())
                || evidence.getOutcomeType() != PregnancyOutcomeType.LIVE_BIRTH
                || evidence.getOutcomeType() != selected.getPregnancyOutcome()) {
            throw notEligible();
        }
        return selected;
    }

    public boolean isEligibleForRead(MotherJourney journey) {
        if (journey.getStatus() != JourneyStatus.ACTIVE
                || journey.getJourneyType() != JourneyType.POSTPARTUM
                || journey.getPregnancyOutcome() != PregnancyOutcomeType.LIVE_BIRTH) return false;
        return evidenceRepository.findFirstByJourneyIdOrderByRevisionNumberDesc(journey.getId())
                .filter(e -> journey.getOwnerUserId().equals(e.getOwnerUserId()))
                .filter(e -> e.getOutcomeType() == PregnancyOutcomeType.LIVE_BIRTH)
                .isPresent();
    }

    public MotherJourney requireEligibleJourneyForRead(UUID journeyId, UUID ownerId) {
        var user=userRepository.findById(ownerId).orElseThrow(this::neutralNotFound);
        if (user.getRole()!=Role.MOTHER) throw new BusinessException(HttpStatus.FORBIDDEN,"LINK_ROLE_REQUIRED","Mother role required");
        var journey=journeyRepository.findById(journeyId).filter(j->ownerId.equals(j.getOwnerUserId())).orElseThrow(this::neutralNotFound);
        var canonical=journeyRepository.findCanonical(ownerId).orElseThrow(this::neutralNotFound);
        if (!journey.getId().equals(canonical.getId())) throw neutralNotFound();
        if (!isEligibleForRead(journey)) throw notEligible();
        return journey;
    }

    private BusinessException neutralNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "LINK_RESOURCE_NOT_FOUND", "Resource not found");
    }

    public static BusinessException notEligible() {
        return new BusinessException(HttpStatus.CONFLICT, "LINK_NOT_ELIGIBLE", "Link is not eligible");
    }
}
