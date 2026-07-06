package com.carebridge.backend.search.service;

import com.carebridge.backend.search.dto.request.SearchRequest;
import com.carebridge.backend.search.dto.response.PaginationMeta;
import com.carebridge.backend.search.dto.response.SearchItemResponse;
import com.carebridge.backend.search.dto.response.SearchResultResponse;
import com.carebridge.backend.search.provider.DomainSearchProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * UC-13 Search and Filter (CB-SEARCH-IMP-013 §5.1 ADR-001). Resolves the {@link
 * DomainSearchProvider} matching {@code request.getType()} and delegates. Request-level
 * validation (SEARCH-001/002/003) already happened in {@code SearchController}.
 */
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final List<DomainSearchProvider> providers;

    @Override
    public SearchResultResponse search(SearchRequest request, UUID userId) {
        DomainSearchProvider provider = providers.stream()
                .filter(p -> p.supports(request.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No DomainSearchProvider registered for type: " + request.getType()));

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<SearchItemResponse> page = provider.search(request.getQ(), userId, pageable);

        return SearchResultResponse.builder()
                .type(request.getType())
                .items(page.getContent())
                .pagination(PaginationMeta.of(page))
                .build();
    }
}
