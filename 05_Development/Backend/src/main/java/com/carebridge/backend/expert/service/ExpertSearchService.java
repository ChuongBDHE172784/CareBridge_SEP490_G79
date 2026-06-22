package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.dto.request.SearchExpertsRequest;
import com.carebridge.backend.expert.dto.response.ExpertSummaryResponse;
import com.carebridge.backend.expert.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExpertSearchService {

    PageResponse<ExpertSummaryResponse> searchExperts(SearchExpertsRequest request);
}
