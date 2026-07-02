package com.carebridge.backend.content;

import com.carebridge.backend.content.dto.request.ImpactReportFilter;
import java.time.LocalDate;

// CASE 2.0 Props Isolation Pattern
class ImpactReportTestFactory {

    static final LocalDate FROM = LocalDate.parse("2026-01-01");
    static final LocalDate TO = LocalDate.parse("2026-06-30");

    static final long MOTHERS = 900L;
    static final long CONSULTATIONS = 1450L;
    static final long ACTIVE_PARTNERS = 28L;
    static final long PUBLISHED_CONTENT = 340L;

    static ImpactReportFilter makeFilter() {
        return new ImpactReportFilter(FROM, TO);
    }

    static ImpactReportFilter makeFilter(LocalDate from, LocalDate to) {
        return new ImpactReportFilter(from, to);
    }
}
