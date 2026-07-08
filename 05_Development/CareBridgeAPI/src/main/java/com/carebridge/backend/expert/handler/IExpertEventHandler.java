package com.carebridge.backend.expert.handler;

public interface IExpertEventHandler {

    void onAnswerExpertPosted(String questionId, String expertId);

    void onCredentialReviewed(String expertProfileId, String newStatus);

}
