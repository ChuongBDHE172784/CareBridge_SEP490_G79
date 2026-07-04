package com.carebridge.backend.search.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.search.dto.request.SearchRequest;
import com.carebridge.backend.search.dto.response.SearchResultResponse;
import com.carebridge.backend.search.entity.SearchType;
import com.carebridge.backend.search.exception.SearchException;
import com.carebridge.backend.search.service.SearchService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-13 Search and Filter — cross-cutting search endpoint (CB-SEARCH-IMP-013 §9).
 * Controller only validates and delegates (CLAUDE.md §Architecture) — no query logic here.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private static final int MAX_QUERY_LENGTH = 200;
    private static final int MAX_SIZE = 50;

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<SearchResultResponse>> search(
            Principal principal,
            @RequestParam String q,
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (q == null || q.isBlank()) {
            throw SearchException.blankQuery();
        }
        if (q.length() > MAX_QUERY_LENGTH) {
            throw SearchException.queryTooLong();
        }
        SearchType searchType;
        try {
            searchType = SearchType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            throw SearchException.invalidType(type);
        }
        if (page < 0) {
            throw SearchException.invalidPage(page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw SearchException.invalidSize(size);
        }

        java.util.UUID userId = SecurityUtils.requireCurrentUserId(principal);

        SearchRequest request = new SearchRequest();
        request.setQ(q);
        request.setType(searchType);
        request.setPage(page);
        request.setSize(size);

        SearchResultResponse response = searchService.search(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
