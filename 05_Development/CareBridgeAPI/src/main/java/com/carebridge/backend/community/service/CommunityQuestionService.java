package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionDetailResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import java.util.UUID;

public interface CommunityQuestionService {

    /**
     * Creates a new community question with status=PENDING.
     * @throws com.carebridge.backend.community.exception.CommunityTopicNotFoundException (COM-003) when topicId is invalid or hidden
     */
    CommunityQuestionResponse createQuestion(UUID authorId, CreateCommunityQuestionRequest request);

    /**
     * Returns full detail of an APPROVED community question including all APPROVED answers,
     * hydrated with the current viewer's bookmark state and per-answer like state.
     * @throws com.carebridge.backend.community.exception.QuestionNotFoundException (COM-006) when question not found or not APPROVED
     */
    CommunityQuestionDetailResponse getQuestionDetail(UUID questionId, UUID currentUserId);

    /**
     * Edits an existing community question.
     * @throws com.carebridge.backend.community.exception.QuestionNotFoundException (COM-006) when question does not exist
     * @throws com.carebridge.backend.community.exception.QuestionNotEditableException (COM-010) when question status is LOCKED or HIDDEN
     * @throws org.springframework.security.access.AccessDeniedException when caller is not the author
     */
    CommunityQuestionResponse editQuestion(UUID authorId, UUID questionId, UpdateCommunityQuestionRequest request);

    PaginatedResponse<CommunityQuestionResponse> getMyQuestions(UUID authorId, int page, int size);

    /**
     * Soft-deletes a community question by setting status = DELETED.
     * @throws com.carebridge.backend.community.exception.QuestionNotFoundException (COM-006) when question does not exist
     * @throws com.carebridge.backend.community.exception.QuestionLockedException (COM-012) when question status is LOCKED
     * @throws org.springframework.security.access.AccessDeniedException when caller is not the author and not a MODERATOR
     */
    void deleteQuestion(UUID questionId, UUID callerId, boolean isModeratorCaller);
}
