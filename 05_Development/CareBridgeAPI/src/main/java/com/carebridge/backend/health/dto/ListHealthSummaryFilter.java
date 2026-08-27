package com.carebridge.backend.health.dto;

import java.time.LocalDate;

public record ListHealthSummaryFilter(
        String summaryPeriod,
        LocalDate from,
        LocalDate to
) {}
