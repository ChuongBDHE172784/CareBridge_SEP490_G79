package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;

public interface CommunityQuestionService {

    /**
     * Creates a new community question with status=PENDING.
     * @throws com.carebridge.backend.community.exception.CommunityTopicNotFoundException (COM-003) when topicId is invalid or hidden
     */
    CommunityQuestionResponse createQuestion(java.util.UUID authorId, CreateCommunityQuestionRequest request);
}
