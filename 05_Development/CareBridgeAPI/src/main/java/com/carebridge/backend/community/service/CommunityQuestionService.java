package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionDetailResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import java.util.UUID;

public interface CommunityQuestionService {

    /**
     * Creates a new community question with status=PENDING.
     * @throws com.carebridge.backend.community.exception.CommunityTopicNotFoundException (COM-003) when topicId is invalid or hidden
     */
    CommunityQuestionResponse createQuestion(UUID authorId, CreateCommunityQuestionRequest request);

    /**
     * Returns full detail of an APPROVED community question including all APPROVED answers.
     * @throws com.carebridge.backend.community.exception.QuestionNotFoundException (COM-006) when question not found or not APPROVED
     */
    CommunityQuestionDetailResponse getQuestionDetail(UUID questionId);

    /**
     * Edits an existing community question.
     * @throws com.carebridge.backend.community.exception.QuestionNotFoundException (COM-006) when question does not exist
     * @throws com.carebridge.backend.community.exception.QuestionNotEditableException (COM-010) when question status is LOCKED or HIDDEN
     * @throws org.springframework.security.access.AccessDeniedException when caller is not the author
     */
    CommunityQuestionResponse editQuestion(UUID authorId, UUID questionId, UpdateCommunityQuestionRequest request);
}
