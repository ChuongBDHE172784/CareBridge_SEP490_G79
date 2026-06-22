package com.carebridge.backend.expert.service.impl;

import com.carebridge.backend.expert.dto.request.SearchExpertsRequest;
import com.carebridge.backend.expert.dto.response.ExpertSummaryResponse;
import com.carebridge.backend.expert.dto.response.PageResponse;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import com.carebridge.backend.expert.repository.ExpertSearchRepository;
import com.carebridge.backend.expert.service.ExpertSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpertSearchServiceImpl implements ExpertSearchService {

    private final ExpertSearchRepository searchRepository;
    private final ExpertProfileMapper profileMapper;

    @Override
    public PageResponse<ExpertSummaryResponse> searchExperts(SearchExpertsRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size);

        Page<ExpertProfile> result = searchRepository.searchVerifiedExperts(
                request.getExpertise(),
                pageable
        );

        List<ExpertSummaryResponse> content = result.getContent().stream()
                .map(profileMapper::toPublicResponse)
                .toList();

        return PageResponse.<ExpertSummaryResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }
}
