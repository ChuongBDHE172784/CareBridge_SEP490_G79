package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.CreateReportRequest;
import com.carebridge.backend.content.dto.response.CreateReportResponse;
import java.util.UUID;

/**
 * UC-14 Report Content or Account (CB-MOD-IMP-014 §8.1).
 */
public interface ReportService {

    /**
     * Creates a new content/account report with status PENDING.
     *
     * @param request        already bean-validated request
     * @param reporterUserId UUID from JWT Principal — never trusted from the request body
     * @throws com.carebridge.backend.content.exception.ReportException RPT-002 target not found,
     *         RPT-003 rate limit exceeded, RPT-004 duplicate pending report,
     *         RPT-005 cannot report own target
     */
    CreateReportResponse createReport(CreateReportRequest request, UUID reporterUserId);
}
