package com.carebridge.backend.search.service;

import com.carebridge.backend.search.dto.request.SearchRequest;
import com.carebridge.backend.search.dto.response.SearchResultResponse;
import java.util.UUID;

public interface SearchService {

    /**
     * Executes a cross-cutting search delegated to the {@code DomainSearchProvider}
     * matching {@code request.getType()}.
     *
     * @param request already-validated (SEARCH-001/002/003 checked upstream in the controller)
     * @param userId   authenticated user id, from JWT
     */
    SearchResultResponse search(SearchRequest request, UUID userId);
}
