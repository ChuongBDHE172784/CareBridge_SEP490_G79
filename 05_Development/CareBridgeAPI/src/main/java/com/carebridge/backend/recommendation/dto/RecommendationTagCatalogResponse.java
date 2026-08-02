package com.carebridge.backend.recommendation.dto;

import java.util.List;
import java.util.UUID;

public record RecommendationTagCatalogResponse(String catalogVersion, List<Item> items) {
    public record Item(UUID id, String slug, String domain, String label) {}
}
