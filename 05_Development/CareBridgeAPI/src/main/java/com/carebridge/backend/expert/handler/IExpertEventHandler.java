package com.carebridge.backend.expert.handler;

public interface IExpertEventHandler {

    /**
     * Called when an expert answer is APPROVED by moderation.
     * Awards contribution points atomically with the approval transition.
     * @param answerId the UUID of the approved answer (used as sourceId for idempotency)
     * @param expertId the userId of the expert author
     */
    void onAnswerApproved(String answerId, String expertId);

    void onCredentialReviewed(String expertProfileId, String newStatus);

}
