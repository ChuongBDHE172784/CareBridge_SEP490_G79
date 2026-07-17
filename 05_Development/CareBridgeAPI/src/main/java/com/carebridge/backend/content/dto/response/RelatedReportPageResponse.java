package com.carebridge.backend.content.dto.response;

import java.util.List;

public record RelatedReportPageResponse(
        List<RelatedReportItemResponse> content,
        long totalElements,
        int page,
        int size
) {}
