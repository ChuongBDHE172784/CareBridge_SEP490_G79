package com.carebridge.backend.community.service;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.request.CommunityQuestionSearchRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionSummaryResponse;

public interface CommunityQuestionSearchService {

    PaginatedResponse<CommunityQuestionSummaryResponse> searchQuestions(CommunityQuestionSearchRequest request);
}
