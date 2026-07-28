package com.carebridge.backend.content.dto.response;

import java.util.List;

/** A paged index of account violation dossiers, one entry per account. */
public record AccountViolationSummaryResponse(
        List<AccountViolationSummaryItemResponse> content,
        long totalElements,
        int page,
        int size
) {}
