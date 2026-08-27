package com.carebridge.backend.expert.handler;

import com.carebridge.backend.expert.service.IContributionPointService;
import com.carebridge.backend.expert.service.IExpertProfileService;
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
    public void onAnswerApproved(String answerId, String expertId) {
        // Award points on APPROVED transition only (not on post)
        // Uses answerId as sourceId for idempotency - duplicate calls with same answerId won't create duplicate points
        try {
            UUID uid = UUID.fromString(expertId);
            UUID sourceId = UUID.fromString(answerId);
            contributionPointService.awardPointsIfNotExists(uid, 5, "Expert answer approved", "EXPERT_ANSWER", sourceId);
            log.debug("Awarded 5 contribution points to expert {} for answer {}", uid, sourceId);
        } catch (Exception e) {
            log.warn("Failed to award contribution points for answer {}: {}", answerId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void onCredentialReviewed(String expertProfileId, String newStatus) {
        // When a credential is reviewed, update the profile status if needed
        try {
            UUID profileId = UUID.fromString(expertProfileId);
            log.debug("Credential reviewed for profile {}: status={}", profileId, newStatus);
        } catch (Exception e) {
            log.warn("Failed to handle credential review event for {}: {}", expertProfileId, e.getMessage());
        }
    }
}