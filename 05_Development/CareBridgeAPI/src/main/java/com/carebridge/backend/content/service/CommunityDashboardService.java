package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.DashboardFilter;
import com.carebridge.backend.content.dto.response.CommunityDashboardResponse;
import com.carebridge.backend.content.exception.ModerationException;

/**
 * Returns aggregate, read-only community metrics for the System Admin dashboard.
 * Response contains no row-level PII — only counts/averages (ADR-004).
 */
public interface CommunityDashboardService {

    /**
     * @throws ModerationException (MOD-021) if filter.from() is after filter.to()
     */
    CommunityDashboardResponse getDashboard(DashboardFilter filter);
}
