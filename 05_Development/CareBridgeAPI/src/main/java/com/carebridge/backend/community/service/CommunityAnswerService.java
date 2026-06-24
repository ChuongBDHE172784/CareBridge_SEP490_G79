package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;

import java.util.UUID;

public interface CommunityAnswerService {

    /**
     * Posts a new community answer to an APPROVED question.
     * @throws com.carebridge.backend.community.exception.QuestionNotAnswerableException (COM-007) when question is not APPROVED
     */
    CommunityAnswerResponse postAnswer(Long authorId, UUID questionId, PostCommunityAnswerRequest request);
}
