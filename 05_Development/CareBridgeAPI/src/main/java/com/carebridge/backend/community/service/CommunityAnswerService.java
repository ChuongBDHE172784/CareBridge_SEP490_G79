package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.request.EditAnswerRequest;
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;

import java.util.UUID;

public interface CommunityAnswerService {

    /**
     * Posts a new community answer to an APPROVED question.
     * @throws com.carebridge.backend.community.exception.QuestionNotAnswerableException (COM-007) when question is not APPROVED
     */
    CommunityAnswerResponse postAnswer(UUID authorId, UUID questionId, PostCommunityAnswerRequest request);

    /**
     * Edits body and isPersonalExperience of the caller's own answer. Resets status to PENDING for re-moderation.
     * @throws com.carebridge.backend.community.exception.AnswerNotFoundException (COM-011) when answer does not exist
     * @throws com.carebridge.backend.community.exception.AnswerNotEditableException (COM-013) when answer status is HIDDEN or DELETED
     * @throws org.springframework.security.access.AccessDeniedException when caller is not the author
     */
    CommunityAnswerResponse editAnswer(UUID answerId, UUID callerId, EditAnswerRequest request);

    /**
     * Soft-deletes an answer by setting status = DELETED. Decrements the parent question's
     * answer_count if the deleted answer was APPROVED.
     * @throws com.carebridge.backend.community.exception.AnswerNotFoundException (COM-011) when answer does not exist
     * @throws org.springframework.security.access.AccessDeniedException when caller is not the author and not a MODERATOR
     */
    void deleteAnswer(UUID answerId, UUID callerId, boolean isModeratorCaller);
}
