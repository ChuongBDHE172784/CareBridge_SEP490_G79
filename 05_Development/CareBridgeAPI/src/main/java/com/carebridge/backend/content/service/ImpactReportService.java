package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ImpactReportFilter;
import com.carebridge.backend.content.dto.response.ImpactReportResponse;
import com.carebridge.backend.content.exception.ModerationException;

/**
 * Returns anonymized, aggregate impact metrics for the given (optional) window.
 * Read-only; response contains no row-level PII (ADR-004). v1 returns system-wide totals only —
 * no dimensional breakdown (small-cohort suppression scope guard, see suppressSmallCohorts()).
 */
public interface ImpactReportService {

    /**
     * @throws ModerationException (MOD-022) if filter.from() is after filter.to()
     */
    ImpactReportResponse getImpactReport(ImpactReportFilter filter);
}
