package com.carebridge.backend.search.dto.response;

import com.carebridge.backend.search.entity.SearchType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {

    private SearchType type;
    private List<SearchItemResponse> items;
    private PaginationMeta pagination;
}
