package com.carebridge.backend.integration.gemini.service;

import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.dto.RagAudienceContext;

public interface RagPolicyService {
    RagAnswerResponse generateAnswer(RagAnswerRequest request, RagAudienceContext context);
}
