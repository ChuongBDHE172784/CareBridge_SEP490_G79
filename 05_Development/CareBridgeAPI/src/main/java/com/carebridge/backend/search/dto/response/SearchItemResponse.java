package com.carebridge.backend.search.dto.response;

import com.carebridge.backend.search.entity.SearchType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchItemResponse {

    private UUID id;
    private String title;
    private String snippet;
    private SearchType type;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
