package com.carebridge.backend.search.provider;

import com.carebridge.backend.search.dto.response.SearchItemResponse;
import com.carebridge.backend.search.entity.SearchType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Strategy interface — one implementation per searchable domain (QUESTION, CONTENT, ...).
 * {@link com.carebridge.backend.search.service.SearchServiceImpl} resolves the matching
 * provider via {@link #supports(SearchType)} and delegates.
 */
public interface DomainSearchProvider {

    boolean supports(SearchType type);

    Page<SearchItemResponse> search(String q, UUID userId, Pageable pageable);
}
