package com.carebridge.backend.content.dto.response;

import java.time.Instant;
import java.time.LocalDate;

// Aggregate-only, anonymized response — no entity fields, no row-level PII, no dimensional breakdown (ADR-004)
public record ImpactReportResponse(
        long mothersServed,
        long consultationsDelivered,
        long activePartnerOrganizations,
        long publishedContentItems,
        LocalDate periodFrom,
        LocalDate periodTo,
        Instant generatedAt,
        String anonymizationNote) {
}
