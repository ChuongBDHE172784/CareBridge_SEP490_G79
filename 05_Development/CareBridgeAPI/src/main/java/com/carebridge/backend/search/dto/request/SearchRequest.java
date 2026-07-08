package com.carebridge.backend.search.dto.request;

import com.carebridge.backend.search.entity.SearchType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequest {

    private String q;
    private SearchType type;
    private int page = 0;
    private int size = 20;
}
