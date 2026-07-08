package com.carebridge.backend.expert.handler;

import com.carebridge.backend.expert.service.IContributionPointService;
import com.carebridge.backend.expert.service.IExpertProfileService;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpertEventHandlerImpl implements IExpertEventHandler {

    private final IContributionPointService contributionPointService;
    private final IExpertProfileService expertProfileService;

    @Override
    @Transactional
    public void onAnswerExpertPosted(String questionId, String expertId) {
        // expertId is the userId (Principal name) from SecurityUtils
        try {
            UUID uid = UUID.fromString(expertId);
            // Award 5 points for each expert answer posted
            contributionPointService.awardPoints(uid, 5, "Posted expert answer: " + questionId, "EXPERT_ANSWER", null);
            log.debug("Awarded 5 contribution points to expert {}", uid);
        } catch (Exception e) {
            log.warn("Failed to award contribution points for answer {}: {}", questionId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void onCredentialReviewed(String expertProfileId, String newStatus) {
        // When a credential is reviewed, update the profile status if needed
        try {
            UUID profileId = UUID.fromString(expertProfileId);
            // If credential is approved, the expert profile may become APPROVED
            // (handled by the credential review flow - UC-70)
            log.debug("Credential reviewed for profile {}: status={}", profileId, newStatus);
        } catch (Exception e) {
            log.warn("Failed to handle credential review event for {}: {}", expertProfileId, e.getMessage());
        }
    }
}
