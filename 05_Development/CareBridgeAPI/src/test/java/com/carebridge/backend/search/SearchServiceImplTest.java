package com.carebridge.backend.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.carebridge.backend.search.dto.request.SearchRequest;
import com.carebridge.backend.search.dto.response.SearchItemResponse;
import com.carebridge.backend.search.dto.response.SearchResultResponse;
import com.carebridge.backend.search.entity.SearchType;
import com.carebridge.backend.search.provider.DomainSearchProvider;
import com.carebridge.backend.search.service.SearchService;
import com.carebridge.backend.search.service.SearchServiceImpl;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * SEARCH-TC-013-001 — happy path dispatch to the matching provider (TDS §5.1, ADR-001).
 * Uses the real {@link SearchServiceImpl} constructor (provider list) once Green; during
 * Red Phase the no-arg stub always throws so these tests must fail first (CASE 2.0 GATE-2).
 */
class SearchServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private SearchItemResponse makeItem(SearchType type) {
        return SearchItemResponse.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000099"))
                .title("Test Question về thai kỳ")
                .snippet("Mình đang mang thai tuần 28 và...")
                .type(type)
                .metadata(new HashMap<>())
                .createdAt(Instant.now())
                .build();
    }

    private SearchRequest makeRequest(SearchType type) {
        SearchRequest req = new SearchRequest();
        req.setQ("thai kỳ");
        req.setType(type);
        req.setPage(0);
        req.setSize(20);
        return req;
    }

    // SEARCH-TC-013-001: valid QUESTION search delegates to matching provider
    @Test
    void search_delegatesToMatchingProvider_returnsMappedResult() {
        DomainSearchProvider questionProvider = mock(DomainSearchProvider.class);
        DomainSearchProvider contentProvider = mock(DomainSearchProvider.class);
        when(questionProvider.supports(SearchType.QUESTION)).thenReturn(true);
        when(contentProvider.supports(SearchType.QUESTION)).thenReturn(false);

        Pageable pageable = PageRequest.of(0, 20);
        Page<SearchItemResponse> providerPage =
                new PageImpl<>(List.of(makeItem(SearchType.QUESTION), makeItem(SearchType.QUESTION),
                        makeItem(SearchType.QUESTION), makeItem(SearchType.QUESTION), makeItem(SearchType.QUESTION)),
                        pageable, 5);
        when(questionProvider.search("thai kỳ", USER_ID, pageable)).thenReturn(providerPage);

        SearchService service = new SearchServiceImpl(List.of(questionProvider, contentProvider));

        SearchResultResponse result = service.search(makeRequest(SearchType.QUESTION), USER_ID);

        assertThat(result.getType()).isEqualTo(SearchType.QUESTION);
        assertThat(result.getItems()).hasSize(5);
        assertThat(result.getPagination().getPage()).isEqualTo(0);
        assertThat(result.getPagination().getTotalElements()).isEqualTo(5);
    }

    // No provider supports the requested type — should not silently return empty; fail loud.
    @Test
    void search_noProviderSupportsType_throwsIllegalState() {
        DomainSearchProvider provider = mock(DomainSearchProvider.class);
        when(provider.supports(any())).thenReturn(false);

        SearchService service = new SearchServiceImpl(List.of(provider));

        assertThatThrownBy(() -> service.search(makeRequest(SearchType.CONTENT), USER_ID))
                .isInstanceOf(IllegalStateException.class);
    }
}
